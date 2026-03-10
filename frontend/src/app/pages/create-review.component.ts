import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ReviewService, ReviewResponseDTO } from '../services/review.service';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { ProductDetailService, ProductDetail } from '../services/product-detail.service';

@Component({
  selector: 'app-create-review',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <app-header></app-header>
      
      <main class="flex-1 container mx-auto px-4 py-8">
        <div class="max-w-2xl mx-auto bg-white rounded-lg shadow p-6">
          <h1 class="text-2xl font-bold mb-6 text-blue-800">{{ pageTitle }}</h1>
          
          <!-- Información del producto -->
          <div *ngIf="product" class="bg-blue-50 rounded p-4 mb-6 flex gap-4">
            <img 
              *ngIf="product.imagenes && product.imagenes.length > 0" 
              [src]="product.imagenes[0].url" 
              alt="Producto"
              class="w-20 h-20 object-contain rounded"
            />
            <div>
              <h2 class="font-bold text-lg">{{ product.nombre }}</h2>
              <p class="text-gray-600">$ {{ product.precio }}</p>
            </div>
          </div>

          <!-- Formulario de reseña -->
          <form (ngSubmit)="submitReview()" class="space-y-6">
            <!-- Rating -->
            <div>
              <label class="block text-sm font-semibold mb-2">Calificación (1-5 estrellas)</label>
              <select 
                [(ngModel)]="rating" 
                name="rating"
                required
                class="w-full p-2 border rounded"
              >
                <option value="">Selecciona una calificación</option>
                <option [value]="1">1 - Muy insatisfecho</option>
                <option [value]="2">2 - Insatisfecho</option>
                <option [value]="3">3 - Neutral</option>
                <option [value]="4">4 - Satisfecho</option>
                <option [value]="5">5 - Muy satisfecho</option>
              </select>
            </div>

            <!-- Comentario -->
            <div>
              <label class="block text-sm font-semibold mb-2">
                Tu comentario ({{ comentario.length }}/1000 caracteres)
              </label>
              <textarea 
                [(ngModel)]="comentario" 
                name="comentario"
                minlength="10"
                maxlength="1000"
                placeholder="Cuéntanos tu experiencia con este producto..."
                rows="5"
                class="w-full p-2 border rounded"
                required
              ></textarea>
              <p class="text-xs text-gray-500 mt-1">
                Mínimo 10 caracteres, máximo 1000
              </p>
            </div>

            <!-- Botones -->
            <div class="flex gap-4">
              <button 
                type="submit" 
                [disabled]="loading"
                class="flex-1 px-6 py-2 bg-blue-600 text-white rounded font-semibold hover:bg-blue-700 transition disabled:bg-gray-400"
              >
                {{ loading ? (isEditing ? 'Actualizando...' : 'Enviando...') : (isEditing ? 'Actualizar reseña' : 'Publicar reseña') }}
              </button>
              <button 
                type="button" 
                (click)="cancelar()"
                class="flex-1 px-6 py-2 bg-gray-300 text-gray-700 rounded font-semibold hover:bg-gray-400 transition"
              >
                Cancelar
              </button>
            </div>

            <div *ngIf="error" class="p-4 bg-red-100 text-red-700 rounded">
              {{ error }}
            </div>
          </form>
        </div>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: []
})
export class CreateReviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private reviewService = inject(ReviewService);
  private productDetailService = inject(ProductDetailService);
  private cdr = inject(ChangeDetectorRef);

  product: ProductDetail | null = null;
  rating: number | null = null;
  comentario: string = '';
  loading = false;
  error: string | null = null;
  productId: number | null = null;
  reviewId: number | null = null;
  isEditing: boolean = false;
  pageTitle: string = 'Escribe tu reseña';

  ngOnInit() {
    this.productId = Number(this.route.snapshot.paramMap.get('productId')) || null;
    const reviewIdParam = this.route.snapshot.queryParamMap.get('reviewId');
    this.reviewId = reviewIdParam ? Number(reviewIdParam) : null;
    this.isEditing = !!this.reviewId;
    this.pageTitle = this.isEditing ? 'Actualizar tu reseña' : 'Escribe tu reseña';
    
    if (this.productId) {
      this.productDetailService.getProductDetail(this.productId).subscribe({
        next: (data) => {
          setTimeout(() => {
            this.product = data;
            this.cdr.markForCheck();
          }, 0);
        },
        error: () => {
          this.error = 'No se pudo cargar el producto';
          this.cdr.markForCheck();
        }
      });

      // Si estamos editando, cargar los datos de la reseña existente
      if (this.isEditing && this.reviewId) {
        const userId = localStorage.getItem('userId');
        this.reviewService.getUserReviewForProduct(this.productId, userId || '').subscribe({
          next: (review) => {
            this.rating = review.rating;
            this.comentario = review.comentario;
            this.cdr.markForCheck();
          },
          error: (err) => {
            this.error = 'No se pudo cargar tu reseña anterior';
            this.cdr.markForCheck();
          }
        });
      }
    }
  }

  submitReview() {
    if (!this.rating || !this.comentario.trim() || this.comentario.length < 10) {
      this.error = 'Completa todos los campos correctamente';
      return;
    }

    if (!this.productId) {
      this.error = 'Producto no identificado';
      return;
    }

    this.loading = true;
    this.error = null;

    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');

    const dto = {
      productId: this.productId,
      rating: this.rating,
      comentario: this.comentario
    };

    if (this.isEditing && this.reviewId) {
      // Actualizar reseña existente
      this.reviewService.updateReview(this.reviewId, dto).subscribe({
        next: () => {
          this.router.navigate(['/product', this.productId]);
        },
        error: (err) => {
          this.error = err.error?.message || 'Error al actualizar la reseña';
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      // Crear nueva reseña
      this.reviewService.createReview(dto, userId || '').subscribe({
        next: () => {
          this.router.navigate(['/product', this.productId]);
        },
        error: (err) => {
          this.error = err.error?.message || 'Error al publicar la reseña';
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  cancelar() {
    this.router.navigate(['/product', this.productId]);
  }
}
