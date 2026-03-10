import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { OrderService } from '../services/order.service';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <app-header></app-header>

      <main class="flex-1 container mx-auto px-4 py-8">
        <h1 class="text-3xl font-bold text-blue-800 mb-8">Mis Reseñas</h1>

        <ng-container *ngIf="loading; else loaded">
          <div class="flex flex-col items-center justify-center min-h-96">
            <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
            <span class="text-blue-700 font-semibold text-lg">Cargando productos...</span>
          </div>
        </ng-container>

        <ng-template #loaded>
          <div *ngIf="products.length > 0; else noProducts" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div *ngFor="let product of products" class="border rounded-lg shadow p-4 bg-white hover:shadow-md transition">
              <!-- Imagen -->
              <div class="mb-4">
                <ng-container *ngIf="product.imagenPrincipal?.url; else noImage">
                  <img [src]="product.imagenPrincipal.url" alt="{{ product.nombre }}" class="w-full h-40 object-contain rounded" />
                </ng-container>
                <ng-template #noImage>
                  <div class="w-full h-40 flex items-center justify-center bg-slate-100 rounded">
                    <span class="text-slate-400 text-sm">Imagen no disponible</span>
                  </div>
                </ng-template>
              </div>

              <!-- Nombre y precio -->
              <h3 class="font-semibold text-lg mb-1">{{ product.nombre }}</h3>
              <p class="text-blue-600 font-bold text-lg mb-4">$ {{ product.precio }}</p>

              <!-- Estado de reseña -->
              <div class="mb-4">
                <span *ngIf="product.hasReview" class="text-xs font-semibold text-green-700 bg-green-100 px-3 py-1 rounded">
                  Ya opinaste
                </span>
                <span *ngIf="!product.hasReview" class="text-xs font-semibold text-orange-700 bg-orange-100 px-3 py-1 rounded">
                  Pendiente de reseña
                </span>
              </div>

              <!-- Botón -->
              <button
                (click)="navigateToReview(product)"
                [class]="product.hasReview ? 'bg-blue-500 hover:bg-blue-600' : 'bg-green-600 hover:bg-green-700'"
                class="w-full text-white px-4 py-2 rounded font-semibold transition"
              >
                <span *ngIf="product.hasReview">Actualizar reseña</span>
                <span *ngIf="!product.hasReview">Escribir reseña</span>
              </button>
            </div>
          </div>

          <!-- Botón Cargar más -->
          <div *ngIf="hasMorePages" class="mt-8 flex justify-center">
            <button
              (click)="loadMoreProducts()"
              [disabled]="loadingMore"
              class="px-6 py-3 bg-gray-600 text-white rounded font-semibold hover:bg-gray-700 transition disabled:bg-gray-400"
            >
              {{ loadingMore ? 'Cargando...' : 'Cargar más' }}
            </button>
          </div>

          <ng-template #noProducts>
            <div class="flex flex-col items-center justify-center min-h-96 text-center">
              <h2 class="text-2xl font-bold text-gray-700 mb-2">No hay productos para reseñar</h2>
              <p class="text-gray-600 mb-6">Aún no has comprado productos o tus órdenes no han sido entregadas.</p>
              <button
                (click)="router.navigate(['/catalog'])"
                class="px-6 py-2 bg-blue-600 text-white rounded font-semibold hover:bg-blue-700 transition"
              >
                Ver catálogo
              </button>
            </div>
          </ng-template>
        </ng-template>

        <div *ngIf="error" class="mt-6 p-4 bg-red-100 text-red-700 rounded font-semibold text-center">
          {{ error }}
        </div>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: []
})
export class MyReviewsComponent implements OnInit {
  private orderService = inject(OrderService);
  private cdr = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  public router = inject(Router);

  loading = true;
  loadingMore = false;
  error: string | null = null;
  products: any[] = [];
  
  // Paginación
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  hasMorePages = false;

  ngOnInit() {
    this.loadReviewableProducts();
  }

  private loadReviewableProducts() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      this.error = 'Debes estar autenticado para ver tus reseñas';
      this.loading = false;
      this.cdr.markForCheck();
      return;
    }

    this.orderService.getReviewableProducts(userId, this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        setTimeout(() => {
          this.products = response.content || [];
          this.totalPages = response.totalPages || 0;
          this.hasMorePages = this.currentPage < this.totalPages - 1;
          this.loading = false;
          this.cdr.markForCheck();
        }, 0);
      },
      error: (err) => {
        console.error('Error loading reviewable products:', err);
        setTimeout(() => {
          this.error = 'No se pudieron cargar los productos disponibles para reseña';
          this.loading = false;
          this.cdr.markForCheck();
        }, 0);
      }
    });
  }

  loadMoreProducts() {
    const userId = localStorage.getItem('userId');
    if (!userId || !this.hasMorePages) return;

    this.loadingMore = true;
    this.currentPage++;

    this.orderService.getReviewableProducts(userId, this.currentPage, this.pageSize).subscribe({
      next: (response: any) => {
        setTimeout(() => {
          this.products = [...this.products, ...(response.content || [])];
          this.totalPages = response.totalPages || 0;
          this.hasMorePages = this.currentPage < this.totalPages - 1;
          this.loadingMore = false;
          this.cdr.markForCheck();
        }, 0);
      },
      error: (err) => {
        console.error('Error loading more products:', err);
        this.currentPage--;
        this.loadingMore = false;
        this.cdr.markForCheck();
      }
    });
  }

  navigateToReview(product: any) {
    const params: any = {};
    if (product.reviewId) {
      params.reviewId = product.reviewId;
    }
    this.router.navigate(['/product', product.id, 'review'], { queryParams: params });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
