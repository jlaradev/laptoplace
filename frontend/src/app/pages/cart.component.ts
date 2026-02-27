import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { take } from 'rxjs/operators';
import { timeout, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { CartService } from '../services/cart.service';
import { ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <div *ngIf="mostrarOverlayError" class="payment-overlay-error" style="position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:9999;display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,0.85);">
        <div class="payment-overlay-content" style="background:white;padding:2rem 2.5rem;border-radius:1rem;box-shadow:0 2px 16px rgba(0,0,0,0.12);text-align:center;">
          <span class="payment-overlay-icon" style="font-size:2.5rem;color:#e53e3e;">&#9888;</span>
          <h2>Ya no hay suficientes unidades disponibles</h2>
          <p>Se están ajustando las unidades del carrito para reflejar el stock disponible.</p>
        </div>
      </div>
      <app-header></app-header>
      <main class="flex-1 container mx-auto px-4 py-8">
        <h1 class="text-2xl font-bold mb-6">Tu carrito</h1>
        <div class="relative">
          <div [class.opacity-60]="loading">
            <div *ngIf="!loading && items.length === 0" class="text-center text-slate-500 py-12">Tu carrito está vacío.</div>
            <div *ngIf="items.length > 0" class="space-y-4">
              <div *ngFor="let item of items" class="flex items-center gap-4 p-4 border rounded">
                <img *ngIf="item.product?.imagenUrl" [src]="item.product.imagenUrl" class="w-20 h-20 object-contain" />
                <div class="flex-1">
                  <div class="font-semibold">{{ item.product?.nombre }}</div>
                  <div class="text-sm text-slate-600">Valor unitario: {{ item.product?.precio | currency:'USD':'symbol':'1.2-2' }}</div>
                  <div class="flex items-center gap-2 mt-2">
                    <label for="cantidad-{{item.id}}" class="text-sm text-slate-600">Cantidad:</label>
                    <input
                      id="cantidad-{{item.id}}"
                      type="number"
                      min="1"
                      [max]="item.product?.stock ?? item.stock"
                      class="w-16 border rounded px-2 py-1"
                      [(ngModel)]="item.editCantidad"
                      (change)="updateQuantity(item)"
                    />
                    <span class="text-xs text-slate-500">Unidades disponibles: {{ item.product?.stock ?? item.stock ?? '—' }}</span>
                  </div>
                </div>
                  <div class="flex items-center gap-4">
                  <div class="font-bold text-blue-700">$ {{ (item.product?.precio * item.cantidad) | number:'1.2-2' }}</div>
                  <button
                    type="button"
                    [ngClass]="item.deleting ? 'cursor-not-allowed' : 'hover:bg-red-100'"
                    class="px-3 py-1 bg-red-50 text-red-700 border border-red-100 rounded flex items-center gap-2"
                    (click)="removeItem(item.id)"
                    [disabled]="item.deleting"
                  >
                    <span>Eliminar</span>
                  </button>
                </div>
              </div>
              <div class="text-right font-semibold mt-4">Total: $ {{ total }}</div>
            </div>
              <div class="text-right mt-6">
                <button
                  class="px-6 py-2 bg-blue-600 text-white rounded font-semibold shadow hover:bg-blue-700 transition"
                  [disabled]="loading || items.length === 0"
                  (click)="goToPayment()"
                >
                  Pagar
                </button>
              </div>
          </div>
          <div *ngIf="loading && firstLoad" class="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div class="text-center text-slate-600 bg-white/70 px-4 py-2 rounded">Cargando...</div>
          </div>
        </div>
      </main>
      <app-footer></app-footer>
    </div>
  `,
  styles: []
})
export class CartPageComponent implements OnInit {
  items: any[] = [];
  total = '0.00';
  loading = true;
  firstLoad = true;
  private cartService = inject(CartService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router) as Router;
    /**
     * Navega a la pantalla de pago, pasando el total y los productos del carrito como state.
     */
    mostrarOverlayError = false;
    error = '';
    goToPayment() {
      // Validar stock antes de continuar
      const productosExcedidos = this.items.filter(item => {
        const stock = item.product?.stock ?? item.stock ?? Infinity;
        return item.cantidad > stock;
      });
      if (productosExcedidos.length > 0) {
        this.mostrarOverlayError = true;
        this.error = 'Ya no hay suficientes unidades disponibles.';
        this.cdr.detectChanges();
        // Ajustar cantidades antes de recargar
        productosExcedidos.forEach(item => {
          const stock = item.product?.stock ?? item.stock ?? 1;
          if (stock === 0) {
            this.cartService.removeFromCart(item.id).subscribe({
              next: () => {},
              error: () => {}
            });
          } else {
            this.cartService.updateItemQuantity(item.id, stock).subscribe({
              next: () => {},
              error: () => {}
            });
          }
        });
        setTimeout(() => {
          window.location.reload();
        }, 5000);
        return;
      }
      // Se pasa el total y los productos (id, cantidad, precio, nombre) como state
      const cartData = {
        total: this.total,
        items: this.items.map(item => ({
          id: item.id,
          cantidad: item.cantidad,
          precio: item.product?.precio ?? item.precio,
          nombre: item.product?.nombre ?? '',
          imagenUrl: item.product?.imagenUrl ?? '',
        }))
      };
      this.router.navigate(['/payment'], { state: cartData });
    }
  // Debounce timers per item to batch rapid quantity changes
  private pendingTimers = new Map<number, any>();
  // Track last requested quantity per item to help ignore out-of-order responses
  private lastRequestedQty = new Map<number, number>();
  // Track ids currently being deleted
  deletingIds = new Set<number>();

  ngOnInit(): void {
    this.load();
    this.cartService.cartChanged$.subscribe((ev: any) => {
      if (!ev || ev.type === 'refresh') {
        this.load();
        return;
      }
      if (ev.type === 'item-updated') {
        const payload = ev.payload;
        const index = this.items.findIndex((i) => i.id === payload.id);
        if (index === -1) return;
        // If the item is currently being updated locally, ignore the incoming update
        if (this.items[index].updating) return;
        // If we have a lastRequestedQty for this item and it differs from payload,
        // it may be an out-of-order response; still apply but prefer local pending state.
        this.items[index] = { ...this.items[index], cantidad: payload.cantidad };
        this.calculateTotal();
        setTimeout(() => { try { this.cdr.markForCheck(); } catch (e) {} }, 0);
      }
    });
  }

  load() {
    if (this.firstLoad) {
      this.loading = true;
    }
    const cached = this.cartService.getCachedCart();
    if (cached) {
      setTimeout(() => this.applyCartData(cached), 0);
    }
    this.cartService.getCart().pipe(
      take(1),
      timeout(5000),
      catchError(() => of({ items: this.items, total: this.total }))
    ).subscribe({
      next: (data: any) => this.applyCartData(data),
      error: () => this.applyCartData({ items: [], total: 0 })
    });
  }

  applyCartData(data: any) {
    setTimeout(() => {
        this.items = (data.items || []).map((it: any) => ({ ...it, editCantidad: it.cantidad, updating: false }));
      const total = data.total || 0;
      this.total = typeof total === 'string' ? parseFloat(total).toFixed(2) : total.toFixed(2);
      this.loading = false;
      this.firstLoad = false;
      try { this.cdr.markForCheck(); } catch (e) { /* ignore */ }
    }, 0);
  }

  updateQuantity(item: any) {
    const stock = item.product?.stock ?? item.stock ?? Infinity;
    // Clamp the cantidad to the allowed range (1..stock) based on editCantidad
    const requested = Number(item.editCantidad) || 1;
    const clamped = Math.min(Math.max(1, Math.floor(requested)), stock);
    if (clamped !== requested) {
      item.editCantidad = clamped;
    }

    // Do not emit optimistic update from the cart page (header is hidden here)

    // Clear any existing timer and debounce rapid changes per item
    const existing = this.pendingTimers.get(item.id);
    if (existing) clearTimeout(existing);
    const t = setTimeout(() => {
      this.sendUpdate(item);
      this.pendingTimers.delete(item.id);
    }, 300);
    this.pendingTimers.set(item.id, t);
  }

  private sendUpdate(item: any) {
    // optimistic: mark as updating and record requested qty; send editCantidad
    item.updating = true;
    const qtyToSend = Number(item.editCantidad) || 1;
    this.lastRequestedQty.set(item.id, qtyToSend);
    this.cartService.updateItemQuantity(item.id, qtyToSend).subscribe({
      next: () => {
        // apply server-confirmed qty locally, clear updating flag
        const index = this.items.findIndex((i) => i.id === item.id);
        if (index !== -1) {
          this.items[index] = { ...this.items[index], cantidad: qtyToSend, editCantidad: qtyToSend };
        }
        item.updating = false;
        this.lastRequestedQty.delete(item.id);
        this.calculateTotal(true);
      },
      error: (err) => {
        console.error('Error al actualizar la cantidad:', err);
        item.updating = false;
        this.lastRequestedQty.delete(item.id);
        // fallback: reload cart to get authoritative state
        this.load();
      }
    });
  }

  calculateTotal(immediate = false) {
    const total = this.items.reduce((sum, item) => {
      const price = item.product?.precio ?? item.precio ?? 0;
      const qty = Number(item.cantidad) || 0;
      return sum + price * qty;
    }, 0);
    const formatted = total.toFixed(2);
    if (immediate) {
      this.total = formatted;
      try { this.cdr.detectChanges(); } catch (e) { /* ignore */ }
    } else {
      setTimeout(() => { this.total = formatted; }, 0);
    }
  }

  removeItem(itemId: number) {
    const item = this.items.find(i => i.id === itemId);
    if (!item || item.deleting) return;
    item.deleting = true;
    this.cartService.removeFromCart(itemId).subscribe({
      next: () => {
        // No eliminamos la fila localmente, solo esperamos a que el backend actualice el carrito
        // y la recarga de datos elimine la fila automáticamente
        this.calculateTotal(true);
      },
      error: (err) => {
        console.error('Error eliminando item del carrito:', err);
        item.deleting = false;
        alert('No se pudo eliminar el item. Intenta nuevamente.');
      }
    });
  }
}
