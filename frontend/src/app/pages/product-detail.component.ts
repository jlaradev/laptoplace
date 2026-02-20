import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductDetailService, ProductDetail } from '../services/product-detail.service';
import { ChangeDetectorRef } from '@angular/core';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <!-- Header -->
      <app-header></app-header>

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
                    <div class="w-full max-w-4xl flex flex-col items-center">
                      <img
                        [src]="imagenesOrdenadas[currentImageIndex].url"
                        alt="Imagen del producto"
                        class="rounded border border-gray-200 shadow-sm object-contain"
                        style="max-width: 1000px; max-height: 700px; min-height: 400px; background: #f8fafc;"
                      >
                      <div class="text-lg text-gray-600 text-center mt-2">
                        {{ imagenesOrdenadas[currentImageIndex].descripcion }}
                      </div>
                    </div>
                    <div class="flex justify-center items-center gap-4 mt-3">
                      <button (click)="showPrevImage()" class="px-3 py-1 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-lg" [disabled]="imagenesOrdenadas.length < 2">&#8592;</button>
                      <span class="text-xs text-slate-500">{{ currentImageIndex + 1 }} / {{ imagenesOrdenadas.length }}</span>
                      <button (click)="showNextImage()" class="px-3 py-1 rounded-full bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-lg" [disabled]="imagenesOrdenadas.length < 2">&#8594;</button>
                    </div>
                  </div>
                </ng-container>
                <ng-template #noImage>
                  <div class="w-full max-w-4xl flex flex-col items-center">
                    <div class="flex items-center justify-center rounded border border-gray-200 shadow-sm object-contain bg-gray-200 text-gray-500"
                      style="max-width: 1000px; max-height: 700px; min-height: 400px; width: 100%; height: 100%;">
                      <span class="text-2xl font-semibold">Imagen no disponible</span>
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
                        <span class="font-bold text-xl">{{ product.promedioRating }}</span>
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
                </div>
                
              </div>
            </div>
            <!-- Reseñas -->
            <div class="mt-10 bg-white rounded-lg shadow p-6">
              <h3 class="text-xl font-bold mb-4 text-blue-800">Reseñas de usuarios</h3>
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
      get imagenesOrdenadas() {
        if (!this.product?.imagenes) return [];
        return [...this.product.imagenes].sort((a, b) => a.orden - b.orden);
      }
    private cdr = inject(ChangeDetectorRef);
  product: ProductDetail | null = null;
  loading = true;
  private productDetailService = inject(ProductDetailService);
  private route = inject(ActivatedRoute);

  currentImageIndex = 0;

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam !== null ? Number(idParam) : null;
    console.log('ID recibido:', id);
    if (id !== null && !isNaN(id)) {
      this.productDetailService.getProductDetail(id).subscribe({
        next: (data) => {
          console.log('Producto recibido:', data);
          this.product = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al cargar producto:', err);
          this.product = null;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      console.warn('ID no válido para detalle de producto');
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  showPrevImage() {
    if (!this.imagenesOrdenadas.length) return;
    this.currentImageIndex = (this.currentImageIndex - 1 + this.imagenesOrdenadas.length) % this.imagenesOrdenadas.length;
  }

  showNextImage() {
    if (!this.imagenesOrdenadas.length) return;
    this.currentImageIndex = (this.currentImageIndex + 1) % this.imagenesOrdenadas.length;
  }
}
