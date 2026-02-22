import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { PaymentService, PaymentResponseDTO } from './payment.service';
import { OrderService } from '../services/order.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

declare var Stripe: any;
const STRIPE_PUBLISHABLE_KEY = (window as any).APP_CONFIG?.stripePublishableKey;

@Component({
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css'],
  standalone: true,
  imports: [CommonModule]
})
export class PaymentComponent implements OnInit {
  clientSecret: string | null = null;
  paymentResponse: PaymentResponseDTO | null = null;
  loading = false;
  error: string | null = null;

  stripe: any;
  elements: any;
  paymentElement: any;

  total: number = 0;
  items: any[] = [];

  @ViewChild('paymentForm') paymentFormRef!: ElementRef;

  constructor(
    private paymentService: PaymentService,
    private orderService: OrderService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('[Payment] ngOnInit - checking history.state');
    const state: any = window.history.state;
    console.log('[Payment] history.state =', state);
    if (state && state.total && state.items) {
      this.total = parseFloat(state.total);
      this.items = state.items;
      console.log('[Payment] loaded cart data', { total: this.total, items: this.items });
    } else {
      this.error = 'No hay datos del carrito. Regresa y selecciona productos.';
      console.warn('[Payment] no cart data in history.state');
    }
  }

  iniciarPago() {
    this.loading = true;
    const userId = localStorage.getItem('userId');
    if (!userId) {
      this.error = 'No hay usuario autenticado.';
      this.loading = false;
      return;
    }
    // Preparar datos para crear la orden
    const direccionEnvio = 'Dirección de prueba'; // Puedes ajustar esto según tu UI
    const orderPayload = { direccionEnvio };
    console.log('[Payment] crear orden - payload:', orderPayload);
    this.orderService.createOrderFromCart(userId, orderPayload).subscribe({
      next: (orderResp) => {
        console.log('[Payment] orden creada:', orderResp);
        // Usar el clientSecret del pago ya creado en el backend
        if (orderResp.payment && orderResp.payment.clientSecret) {
          this.paymentResponse = orderResp.payment;
          this.clientSecret = orderResp.payment.clientSecret;
          this.loading = false;
          this.cdr.detectChanges();
          setTimeout(() => this.setupPaymentElement(), 0);
        } else {
          this.error = 'No se pudo obtener el clientSecret del pago.';
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('[Payment] error al crear orden', err);
        this.error = 'Error al crear la orden';
        this.loading = false;
      }
    });
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
      console.log('[StripeElement] Payment Element montado correctamente en #payment-form');
    } catch (e) {
      console.error('[StripeElement] Error Stripe Elements:', e);
      this.error = 'Error inicializando Stripe Elements';
    }
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
    this.loading = true;
    try {
      const result = await this.stripe.confirmPayment({
        elements: this.elements,
        confirmParams: { return_url: window.location.href }
      });
      console.log('[Payment] confirmPayment result', result);
      this.loading = false;
      if (result.error) {
        this.error = result.error.message;
      } else if (result.paymentIntent && result.paymentIntent.status === 'succeeded') {
        this.error = null;
        alert('Pago exitoso!');
      } else {
        this.error = 'Pago no completado.';
      }
    } catch (e: any) {
      console.error('[Payment] confirmarPago exception', e);
      this.loading = false;
      this.error = e?.message ?? 'Error inesperado';
    }
  }
}