import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-products-eliminados-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
      <div class="bg-white rounded-lg shadow-lg w-full max-w-3xl p-6 relative">
        <button (click)="close()" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold">×</button>
        <h2 class="text-2xl font-bold mb-4">Productos Eliminados</h2>
        <ng-container *ngIf="products && products.length > 0; else empty">
          <table class="w-full mb-4">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Marca</th>
                <th>Precio</th>
                <th>Stock</th>
                <th>Rating</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let product of products">
                <td>{{ product.id }}</td>
                <td>{{ product.nombre }}</td>
                <td>{{ product.brand?.nombre }}</td>
                <td>{{ product.precio | number:'1.2-2' }}</td>
                <td>{{ product.stock }}</td>
                <td>⭐ {{ product.promedioRating.toFixed(1) }}</td>
                <td>
                  <button (click)="reactivate.emit(product.id)" class="px-3 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200 transition">♻️ Reactivar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </ng-container>
        <ng-template #empty>
          <div class="text-center text-gray-600 py-8">No hay productos eliminados</div>
        </ng-template>
      </div>
    </div>
  `,
  styleUrls: ['./products-eliminados-modal.component.css']
})
export class ProductsEliminadosModalComponent {
  @Input() products: Product[] = [];
  @Output() closeModal = new EventEmitter<void>();
  @Output() reactivate = new EventEmitter<number>();

  close() {
    this.closeModal.emit();
  }
}
