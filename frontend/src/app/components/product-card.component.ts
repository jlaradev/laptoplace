import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../models/product.model';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <a [routerLink]="['/product', product.id]" class="border rounded-lg p-4 shadow-sm cursor-pointer hover:shadow-md transition block bg-white">
      <ng-container *ngIf="product.imagenPrincipal?.url; else noImage">
        <img [src]="product.imagenPrincipal?.url" alt="{{ product.nombre }}" class="w-full h-40 object-contain mb-3" />
      </ng-container>
      <ng-template #noImage>
        <div class="w-full h-40 flex items-center justify-center bg-slate-100 mb-3">
          <span class="text-slate-400 text-sm">Imagen no disponible</span>
        </div>
      </ng-template>
      <div class="mb-2">
        <ng-container *ngIf="product.promedioRating > 0; else noRating">
          <span class="flex items-center space-x-1">
            <span class="text-yellow-500 text-lg">&#9733;</span>
            <span class="font-bold">{{ product.promedioRating }}</span>
            <span class="text-gray-500">/ 5</span>
          </span>
        </ng-container>
        <ng-template #noRating>
          <br>
        </ng-template>
      </div>
      <h3 class="font-semibold text-lg mb-1">{{ product.nombre }}</h3>
      <p class="text-slate-600 mb-2">{{ product.marca }}</p>
      <div *ngIf="product.stock > 0" class="text-xs text-green-700 font-semibold mb-2">
        {{ product.stock }} disponible{{ product.stock !== 1 ? 's' : '' }}
      </div>
      <div *ngIf="product.stock <= 0" class="text-xs text-red-700 font-semibold mb-2">
        Agotado
      </div>
      <p class="font-bold text-blue-600 text-xl">$ {{ product.precio }}</p>
    </a>
  `
})
export class ProductCardComponent {
  @Input() product!: Product;
}
