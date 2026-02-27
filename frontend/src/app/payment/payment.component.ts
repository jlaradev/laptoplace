// ...existing imports...
import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { PaymentService, PaymentResponseDTO } from './payment.service';
import { OrderService } from '../services/order.service';
import { UserService, User } from '../services/user.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';

declare var Stripe: any;
const STRIPE_PUBLISHABLE_KEY = (window as any).APP_CONFIG?.stripePublishableKey;

@Component({
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
})
export class PaymentComponent implements OnInit {
  paymentMethodSelected: boolean = false;
  public toastMsg: string | null = null;
  public toastVisible: boolean = false;

  onStripeElementChange(event: any) {
    this.paymentMethodSelected = !!event.complete;
    if (event.complete && event.value && event.value.type) {
      console.log('[Stripe] Método de pago seleccionado:', event.value.type, event.value);
    }
    this.cdr.detectChanges();
  }
  clientSecret: string | null = null;
  paymentResponse: PaymentResponseDTO | null = null;
  loading = false;
  error: string | null = null;

  stripe: any;
  elements: any;
  paymentElement: any;

  total: number = 0;
  items: any[] = [];

  direccionEnvio: string = '';
  direccionConfirmada: boolean = false;
  mostrarFormularioDireccion: boolean = false;
  direccionUsuario: string = '';
  userId: string | null = null;
  userLoaded: boolean = false;

  @ViewChild('paymentForm') paymentFormRef!: ElementRef;

  constructor(
    private paymentService: PaymentService,
    private orderService: OrderService,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const state: any = window.history.state;
    if (state && state.total && state.items) {
      this.total = parseFloat(state.total);
      this.items = state.items;
      this.userId = localStorage.getItem('userId');
      if (!this.userId) {
        this.error = 'No hay usuario autenticado.';
        return;
      }
      // Obtener dirección del usuario
      this.userService.getUserById(this.userId).subscribe({
        next: (user: User) => {
          this.direccionUsuario = user.direccion || '';
          this.direccionEnvio = this.direccionUsuario;
          this.userLoaded = true;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = 'No se pudo obtener la dirección del usuario.';
          this.userLoaded = true;
        }
      });
    } else {
      this.error = 'No hay datos del carrito. Regresa y selecciona productos.';
    }
  }

  iniciarPago() {
    if (!this.userId) {
      this.error = 'No hay usuario autenticado.';
      return;
    }
    // No activar loading aquí, solo al confirmar pago
    const orderPayload = { direccionEnvio: this.direccionEnvio };
    this.orderService.createOrderFromCart(this.userId, orderPayload).subscribe({
      next: (orderResp) => {
        if (orderResp.payment && orderResp.payment.clientSecret) {
          this.paymentResponse = orderResp.payment;
          this.clientSecret = orderResp.payment.clientSecret;
          this.direccionConfirmada = true;
          this.cdr.detectChanges();
          setTimeout(() => {
            this.setupPaymentElement();
            this.loading = false;
          }, 0);
        } else {
          this.error = 'No se pudo obtener el clientSecret del pago.';
          this.loading = false;
        }
      },
      error: (err) => {
        // Detectar error de stock insuficiente
        const msg = err?.error?.message || err?.error || '';
        if (msg && msg.toLowerCase().includes('stock')) {
          this.mostrarOverlayError = true;
          this.error = 'Ya no hay suficientes unidades disponibles.';
          this.cdr.detectChanges();
          setTimeout(() => {
            this.router.navigate(['/cart']);
          }, 5000);
        } else {
          this.error = 'Error al crear la orden';
        }
        this.loading = false;
      }
    });
  }

  usarOtraDireccion() {
    this.mostrarFormularioDireccion = true;
    this.direccionEnvio = '';
  }

  cancelarOtraDireccion() {
    this.mostrarFormularioDireccion = false;
    this.direccionEnvio = this.direccionUsuario;
  }

  setupPaymentElement() {
    console.log('[StripeElement] setupPaymentElement start', { clientSecret: this.clientSecret });

    if (!this.clientSecret) {
      this.error = 'No se pudo obtener clientSecret para Stripe.';
      console.error('[StripeElement] FALTA clientSecret');
      return;
    }

    if (typeof Stripe === 'undefined') {
      this.error = 'Stripe.js no está cargado';
      console.error('[StripeElement] Stripe.js NO está cargado en window');
      return;
    }

    const formDiv = this.paymentFormRef?.nativeElement;
    if (!formDiv) {
      this.error = 'No se encontró el contenedor del formulario.';
      console.error('[StripeElement] paymentFormRef no disponible en el DOM');
      return;
    }

    try {
      console.log('[StripeElement] Inicializando Stripe con publishableKey:', STRIPE_PUBLISHABLE_KEY);
      this.stripe = Stripe(STRIPE_PUBLISHABLE_KEY);
      this.elements = this.stripe.elements({ clientSecret: this.clientSecret });
      formDiv.innerHTML = '';
      this.paymentElement = this.elements.create('payment');
      this.paymentElement.mount(formDiv);
      // Escuchar cambios en el Payment Element para saber si hay método de pago seleccionado
      this.paymentElement.on('change', (event: any) => this.onStripeElementChange(event));
      this.paymentMethodSelected = false;
      this.loading = false;
      console.log('[StripeElement] Payment Element montado correctamente en #payment-form');
    } catch (e) {
      console.error('[StripeElement] Error Stripe Elements:', e);
      this.error = 'Error inicializando Stripe Elements';
    }
  }

  mostrarOverlayError = false;
  mostrarOverlayAprobada = false;

  public showToast(message: string, isError: boolean = false): void {
    this.toastMsg = message;
    this.toastVisible = true;
    setTimeout(() => {
      this.toastVisible = false;
      setTimeout(() => { this.toastMsg = null; }, 300);
    }, 2500);
  }

  async confirmarPago() {
    console.log('[Payment] confirmarPago invoked');
    if (!this.stripe || !this.clientSecret) {
      console.error('[Payment] cannot confirm - stripe o clientSecret faltante', {
        stripe: !!this.stripe,
        clientSecret: this.clientSecret
      });
      return;
    }
    if (!this.paymentMethodSelected) {
      this.showToast('Debe proporcionar el método de pago', true);
      return;
    }
    this.loading = true;
    this.error = null;
    try {
      const result = await this.stripe.confirmPayment({
        elements: this.elements,
        confirmParams: {
          return_url: window.location.href
        },
        redirect: 'if_required'
      });
      console.log('[Payment] confirmPayment result', result);
      this.loading = false;
      if (result.error) {
        setTimeout(() => {
          this.mostrarOverlayError = true;
          this.cdr.detectChanges();
          setTimeout(() => {
            this.router.navigate(['/cart']);
          }, 5000);
        }, 3000);
      } else if (result.paymentIntent && result.paymentIntent.status === 'succeeded') {
        setTimeout(() => {
          this.mostrarOverlayAprobada = true;
          this.cdr.detectChanges();
          setTimeout(() => {
            this.router.navigate(['/cart']);
          }, 2500);
        }, 300);
      } else {
        setTimeout(() => {
          this.mostrarOverlayError = true;
          this.cdr.detectChanges();
          setTimeout(() => {
            this.router.navigate(['/cart']);
          }, 5000);
        }, 3000);
      }
    } catch (e: any) {
      console.error('[Payment] confirmarPago exception', e);
      this.loading = false;
      setTimeout(() => {
        this.mostrarOverlayError = true;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.router.navigate(['/cart']);
        }, 5000);
      }, 3000);
    }
  }
}