import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Brand } from '../../models/brand.model';

@Component({
  selector: 'app-brands-eliminados-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
      <div class="bg-white rounded-lg shadow-lg w-full max-w-3xl p-6 relative">
        <button (click)="close()" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold">×</button>
        <h2 class="text-2xl font-bold mb-4">Marcas Eliminadas</h2>
        <ng-container *ngIf="brands && brands.length > 0; else empty">
          <table class="w-full mb-4">
            <thead>
              <tr>
                <th>ID</th>
                <th>Logo</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let brand of brands">
                <td>{{ brand.id }}</td>
                <td>
                  <img *ngIf="brand.imageUrl" [src]="brand.imageUrl" [alt]="brand.nombre" class="w-12 h-12 object-cover rounded" />
                  <span *ngIf="!brand.imageUrl" class="text-gray-500 text-xs">Sin logo</span>
                </td>
                <td>{{ brand.nombre }}</td>
                <td class="max-w-xs truncate">{{ brand.descripcion || '—' }}</td>
                <td>
                  <button (click)="reactivate.emit(brand.id)" class="px-3 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200 transition">♻️ Reactivar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </ng-container>
        <ng-template #empty>
          <div class="text-center text-gray-600 py-8">No hay marcas eliminadas</div>
        </ng-template>
      </div>
    </div>
  `,
  styles: []
})
export class BrandsEliminadosModalComponent {
  @Input() brands: Brand[] = [];
  @Output() closeModal = new EventEmitter<void>();
  @Output() reactivate = new EventEmitter<number>();

  close() {
    this.closeModal.emit();
  }
}
