import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductDetailService, ProductDetail } from '../services/product-detail.service';
import { ChangeDetectorRef } from '@angular/core';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { CartService } from '../services/cart.service';
import { AuthService } from '../services/auth.service';
import { OrderService } from '../services/order.service';
import { signal } from '@angular/core';
import { map, catchError } from 'rxjs/operators';
import { of, Observable } from 'rxjs';
import { RoundDecimalPipe } from '../pipes/round-decimal.pipe';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, RoundDecimalPipe],
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <!-- Header -->
      <app-header></app-header>

      <!-- Toast flotante (debajo del header, sin taparlo) -->
      <div class="fixed right-6 z-50 top-20 pointer-events-none">
        <div *ngIf="toastVisible()" class="min-w-[200px] max-w-sm bg-blue-600 text-white px-4 py-3 rounded shadow-lg transition-opacity duration-300">
          {{ toastMsg }}
        </div>
      </div>

      <main class="flex-1 container mx-auto px-4 py-8">
        <ng-container *ngIf="loading; else loaded">
          <div class="flex flex-col items-center justify-center min-h-40">
            <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
            <span class="text-blue-700 font-semibold text-lg">Cargando detalles...</span>
          </div>
        </ng-container>
        <ng-template #loaded>
          <ng-container *ngIf="product; else notfound">
            <div class="bg-white rounded-lg shadow p-6 grid grid-cols-1 md:grid-cols-2 gap-8">
              <!-- Galería de imágenes -->
              <div>
                <ng-container *ngIf="imagenesOrdenadas && imagenesOrdenadas.length; else noImage">
                  <div class="relative flex flex-col items-center">
                    <!-- Contenedor con tamaño fijo para evitar layout shift -->
                    <div class="w-[30rem] h-[30rem] bg-gray-50 rounded border border-gray-200 shadow-sm flex items-center justify-center overflow-hidden">
                      <img
                        [src]="imagenesOrdenadas[currentImageIndex].url"
                        alt="Imagen del producto"
                        class="w-full h-full object-contain"
                      >
                    </div>
                    <!-- Descripción de la imagen -->
                    <div class="text-lg text-gray-600 text-center mt-4 min-h-10">
                      {{ imagenesOrdenadas[currentImageIndex].descripcion }}
                    </div>
                    <!-- Controles de navegación -->
                    <div class="flex justify-center items-center gap-4 mt-4">
                      <button (click)="showPrevImage()" class="px-3 py-1 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-lg" [disabled]="imagenesOrdenadas.length < 2">&#8592;</button>
                      <span class="text-xs text-slate-500">{{ currentImageIndex + 1 }} / {{ imagenesOrdenadas.length }}</span>
                      <button (click)="showNextImage()" class="px-3 py-1 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-lg" [disabled]="imagenesOrdenadas.length < 2">&#8594;</button>
                    </div>
                  </div>
                </ng-container>
                <ng-template #noImage>
                  <!-- Contenedor con tamaño fijo para "sin imagen" también -->
                  <div class="flex flex-col items-center">
                    <div class="w-[30rem] h-[30rem] bg-gray-200 rounded border border-gray-200 shadow-sm flex items-center justify-center">
                      <span class="text-2xl font-semibold text-gray-500">Imagen no disponible</span>
                    </div>
                  </div>
                </ng-template>
              </div>
              <!-- Información principal -->
              <div class="flex flex-col justify-between">
                <div>
                  <div class="mb-2">
                    <ng-container *ngIf="product.resenas && product.resenas.length > 0; else noRating">
                      <span class="flex items-center space-x-2">
                        <span class="text-yellow-500 text-2xl">&#9733;</span>
                        <span class="font-bold text-xl">{{ product.promedioRating | roundDecimal: 1 }}</span>
                        <span class="text-gray-500">/ 5</span>
                      </span>
                    </ng-container>
                    <ng-template #noRating>
                      <br>
                    </ng-template>
                  </div>
                  <h2 class="text-2xl font-bold mb-2">{{ product.nombre }}</h2>
                  <div class="text-lg text-blue-800 font-semibold mb-2">{{ product.marca }}</div>
                  <div class="text-gray-700 mb-4">{{ product.descripcion }}</div>
                  <div class="text-3xl font-bold text-green-600 mb-4">$ {{ product.precio }}</div>
                  <div class="mb-2"><span class="font-semibold">Stock:</span> {{ product.stock }}</div>
                  <div class="mb-2"><span class="font-semibold">Procesador:</span> {{ product.procesador }}</div>
                  <div class="mb-2"><span class="font-semibold">RAM:</span> {{ product.ram }}</div>
                  <div class="mb-2"><span class="font-semibold">Almacenamiento:</span> {{ product.almacenamiento }}</div>
                  <div class="mb-2"><span class="font-semibold">Pantalla:</span> {{ product.pantalla }}</div>
                  <div class="mb-2"><span class="font-semibold">GPU:</span> {{ product.gpu }}</div>
                  <div class="mb-2"><span class="font-semibold">Peso:</span> {{ product.peso }} kg</div>
                  <div *ngIf="product && product.stock > 0" class="mt-6 flex items-center gap-3">
                    <div class="flex items-center border rounded overflow-hidden" [ngClass]="isDisabled() ? 'opacity-60 pointer-events-none bg-gray-100' : 'bg-white'">
                      <button
                        (click)="decreaseQty()"
                        [disabled]="isDisabled() || quantity() <= 1"
                        class="px-3 py-2 text-lg"
                        [ngClass]="isDisabled() ? 'text-gray-400 cursor-not-allowed bg-gray-100' : 'text-slate-700 hover:bg-slate-100'">
                        −
                      </button>
                      <input
                        type="number"
                        [value]="quantity()"
                        (input)="onQtyChange($any($event.target).value)"
                        [min]="1"
                        [max]="product.stock"
                        [disabled]="isDisabled()"
                        class="w-20 text-center px-2 py-2 outline-none"
                        [ngClass]="isDisabled() ? 'bg-gray-100 text-gray-500' : ''"
                        aria-label="Cantidad"
                      />
                      <button
                        (click)="increaseQty()"
                        [disabled]="isDisabled() || quantity() >= product.stock"
                        class="px-3 py-2 text-lg"
                        [ngClass]="isDisabled() ? 'text-gray-400 cursor-not-allowed bg-gray-100' : 'text-slate-700 hover:bg-slate-100'">
                        +
                      </button>
                    </div>
                    <button (click)="addToCart()" [disabled]="isDisabled()" [attr.aria-disabled]="isDisabled()" class="flex-1 px-6 py-3 rounded-full font-bold text-lg shadow transition"
                      [ngClass]="isDisabled() ? 'bg-gray-300 text-slate-600 cursor-not-allowed' : 'bg-blue-600 text-white hover:bg-blue-700'">
                      <span *ngIf="!isInCart() && !loading">Añadir al carrito</span>
                      <span *ngIf="isInCart()">Producto en el carrito</span>
                      <span *ngIf="!isInCart() && loading">Cargando...</span>
                    </button>
                  </div>
                  
                </div>
                
              </div>
            </div>
            <!-- Reseñas -->
            <div class="mt-10 bg-white rounded-lg shadow p-6">
              <div class="flex justify-between items-center mb-4 h-10">
                <h3 class="text-xl font-bold text-blue-800">Reseñas de usuarios</h3>
                <div class="flex flex-col sm:flex-row gap-2 sm:gap-4 items-center">
                  <div *ngIf="userHasPurchased()" class="text-sm text-gray-700 font-medium mb-2 sm:mb-0 order-1 sm:order-1 w-full sm:w-auto">
                    <span *ngIf="!userHasReview()">Ya has comprado este producto</span>
                    <span *ngIf="userHasReview()">Ya has opinado sobre este producto</span>
                  </div>
                  <button 
                    *ngIf="userHasPurchased()"
                    (click)="router.navigate(['/product', product.id, 'review'], { queryParams: { reviewId: existingReviewId() || undefined } })"
                    class="px-6 py-2 bg-blue-600 text-white rounded font-semibold hover:bg-blue-700 transition whitespace-nowrap h-10 order-2 sm:order-2 w-full sm:w-auto">
                    <span *ngIf="!userHasReview()">Opinar sobre este producto</span>
                    <span *ngIf="userHasReview()">Actualizar mi reseña</span>
                  </button>
                </div>
              </div>
              <ng-container *ngIf="product.resenas && product.resenas.length > 0; else noResenas">
                <div class="divide-y divide-gray-200">
                  <div *ngFor="let resena of product.resenas" class="py-4">
                    <div class="flex items-center mb-1">
                      <span class="font-semibold text-blue-700 mr-2">{{ resena.userNombre }}</span>
                      <span class="text-yellow-500">&#9733;</span>
                      <span class="ml-1">{{ resena.rating }}</span>
                    </div>
                    <div class="text-gray-500 text-sm mb-1">{{ resena.createdAt | date:'mediumDate' }}</div>
                    <div class="text-gray-700">{{ resena.comentario }}</div>
                  </div>
                </div>
              </ng-container>
              <ng-template #noResenas>
                <div class="text-center text-slate-500 text-lg py-8">Este producto actualmente no tiene reseñas.</div>
              </ng-template>
            </div>
          </ng-container>
          <ng-template #notfound>
            <div class="text-center text-slate-500 text-lg py-12">Producto no encontrado.</div>
          </ng-template>
        </ng-template>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: []
})
export class ProductDetailComponent implements OnInit {
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private orderService = inject(OrderService);
  toastMsg: string | null = null;
  toastVisible = signal<boolean>(false);
  isLoggedIn = false;
  quantity = signal<number>(1);
  isInCart = signal<boolean>(false);
  private processing = signal<boolean>(false);
  hasPurchased = signal<boolean>(false);
  checkingPurchase = signal<boolean>(false);
  existingReviewId = signal<number | null>(null);
      get imagenesOrdenadas() {
        if (!this.product?.imagenes) return [];
        return [...this.product.imagenes].sort((a, b) => a.orden - b.orden);
      }
    private cdr = inject(ChangeDetectorRef);
  product: ProductDetail | null = null;
  loading = true;
  private productDetailService = inject(ProductDetailService);
  private route = inject(ActivatedRoute);
  router = inject(Router);

  currentImageIndex = 0;

  ngOnInit() {
    this.isLoggedIn = this.authService.isLoggedInSync();
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam !== null ? Number(idParam) : null;
    if (id !== null && !isNaN(id)) {
      this.productDetailService.getProductDetail(id).subscribe({
        next: (data) => {
          this.product = data;
          this.quantity.set(1);
          // Verificar si el usuario ha comprado este producto y si ya tiene reseña
          if (this.isLoggedIn) {
            const userId = localStorage.getItem('userId');
            if (userId) {
              this.checkingPurchase.set(true);
              this.orderService.isProductPurchased(userId, id).subscribe({
                next: (resp) => {
                  this.hasPurchased.set(resp.purchased);
                  this.existingReviewId.set(resp.reviewId ?? null);
                  this.checkingPurchase.set(false);
                  this.cdr.detectChanges();
                },
                error: () => {
                  this.hasPurchased.set(false);
                  this.checkingPurchase.set(false);
                  this.cdr.detectChanges();
                }
              });
            }
          }
          // Esperamos a validar si el producto está en el carrito antes de mostrar
          this.checkIfInCart().subscribe({
            next: (found) => {
              this.isInCart.set(found);
              this.loading = false;
              this.cdr.detectChanges();
            },
            error: () => {
              this.isInCart.set(false);
              this.loading = false;
              this.cdr.detectChanges();
            }
          });
        },
        error: (err) => {
          this.product = null;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  addToCart() {
    if (!this.product) return;
    if (this.loading) return;
    if (this.isInCart() || this.processing()) return;
    // If user is not logged in, redirect to login page and stop.
    if (!this.authService.isLoggedInSync()) {
      const redirect = this.router.url || `/product/${this.product.id}`;
      this.router.navigate(['/login'], { queryParams: { redirect } });
      return;
    }
    // Optimistic: deshabilitar UI inmediatamente y notificar al header
    this.processing.set(true);
    this.isInCart.set(true);
    try { this.cartService.notifyCartChanged(); } catch { /* noop */ }

    const qty = Math.max(1, Math.min(this.quantity(), this.product.stock));
    this.cartService.addToCart(this.product.id, qty).subscribe({
      next: () => {
        this.showToast('Producto añadido al carrito');
        this.processing.set(false);
        // cartChanged already emitted by service tap
      },
      error: () => {
        this.showToast('No se pudo añadir al carrito', true);
        this.isInCart.set(false);
        this.processing.set(false);
        try { this.cartService.notifyCartChanged(); } catch { /* noop */ }
      }
    });
  }

  showToast(message: string, isError: boolean = false) {
    this.toastMsg = message;
    this.toastVisible.set(true);
    // Opcional: estilos diferentes para error si quieres
    setTimeout(() => {
      this.toastVisible.set(false);
      setTimeout(() => this.toastMsg = null, 300);
    }, 2500);
  }

  checkIfInCart(): Observable<boolean> {
    if (!this.isLoggedIn || !this.product) return of(false);
    return this.cartService.getCart().pipe(
      map((dataRaw: any) => {
        const data: any = dataRaw;
        let items: any[] = [];
        if (!data) items = [];
        else if (Array.isArray(data)) items = data;
        else if (data.items && Array.isArray(data.items)) items = data.items;
        else items = [];
        const found = items.some((it: any) => {
          if (it.product && (it.product.id !== undefined)) return Number(it.product.id) === Number(this.product!.id);
          if (it.productId !== undefined) return Number(it.productId) === Number(this.product!.id);
          return false;
        });
        return !!found;
      }),
      catchError(() => of(false))
    );
  }

  onQtyChange(val: string | number) {
    const n = Number(val);
    if (isNaN(n)) return;
    const clamped = Math.max(1, Math.min(n, this.product ? this.product.stock : n));
    this.quantity.set(Math.trunc(clamped));
  }

  increaseQty() {
    const max = this.product ? this.product.stock : Number.MAX_SAFE_INTEGER;
    this.quantity.update(q => Math.min(q + 1, max));
  }

  decreaseQty() {
    this.quantity.update(q => Math.max(1, q - 1));
  }

  isDisabled(): boolean {
    return this.loading || this.isInCart() || this.processing() || !this.product;
  }

  showPrevImage() {
    if (!this.imagenesOrdenadas.length) return;
    this.currentImageIndex = (this.currentImageIndex - 1 + this.imagenesOrdenadas.length) % this.imagenesOrdenadas.length;
  }

  showNextImage() {
    if (!this.imagenesOrdenadas.length) return;
    this.currentImageIndex = (this.currentImageIndex + 1) % this.imagenesOrdenadas.length;
  }

  userHasPurchased(): boolean {
    return this.isLoggedIn && this.hasPurchased();
  }

  userHasReview(): boolean {
    return this.existingReviewId() !== null;
  }
}
