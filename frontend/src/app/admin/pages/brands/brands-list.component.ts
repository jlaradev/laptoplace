import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BrandService } from '../../../services/brand.service';
import { AdminComponentsModule } from '../../components/admin-components.module';
import { Brand, BrandPage } from '../../../models/brand.model';

@Component({
  selector: 'app-brands-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, AdminComponentsModule],
  template: `<div class="space-y-6">
  <!-- Alertas amigables -->
  <div *ngIf="showAlert() as alert" class="fixed top-6 left-1/2 transform -translate-x-1/2 z-50">
    <div [ngClass]="{
      'bg-green-100 text-green-800 border-green-200': alert.type === 'success',
      'bg-red-100 text-red-800 border-red-200': alert.type === 'error'
    }" class="border rounded-lg px-6 py-3 shadow-lg text-lg font-medium">
      {{ alert.message }}
      <button class="ml-4 text-xl font-bold text-gray-500 hover:text-gray-700" (click)="showAlert.set(null)">×</button>
    </div>
  </div>

  <!-- Confirmación amigable -->
  <div *ngIf="showConfirm() as confirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
    <div class="bg-white rounded-lg shadow-lg p-6 max-w-md w-full relative">
      <button class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold" (click)="showConfirm.set(null)">×</button>
      <div class="text-lg font-medium mb-4">{{ confirm.message }}</div>
      <div class="flex justify-end gap-2">
        <button class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition" (click)="showConfirm.set(null)">Cancelar</button>
        <button class="px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition" (click)="confirm.onConfirm(); showConfirm.set(null)">Confirmar</button>
      </div>
    </div>
  </div>

  <!-- Header -->
    <div class="flex items-center justify-between">
    <h1 class="text-3xl font-bold text-gray-900">Gestión de Marcas</h1>
    <div class="flex gap-2">
      <a
        routerLink="/admin/brands/create"
        class="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
      >
        ➕ Nueva Marca
      </a>
      <button
        (click)="openEliminadosModal()"
        class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition"
      >
        Ver eliminados
      </button>
      <app-brands-eliminados-modal
        *ngIf="showEliminadosModal()"
        [brands]="inactiveBrands()"
        (closeModal)="closeEliminadosModal()"
        (reactivate)="reactivateBrand($event)"
      ></app-brands-eliminados-modal>
    </div>
  </div>

  <!-- Search -->
  <div class="bg-white border border-gray-200 rounded-lg p-4">
    <div class="flex gap-4">
      <input
        type="text"
        [(ngModel)]="searchTerm"
        placeholder="Buscar marcas por nombre..."
        class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
        (click)="search()"
        class="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
      >
        Buscar
      </button>
      <button
        (click)="reset()"
        class="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition"
      >
        Limpiar
      </button>
    </div>
  </div>

  <!-- Loading -->
  <div *ngIf="isLoading()" class="text-center py-12">
    <p class="text-gray-600">Cargando marcas...</p>
  </div>

  <!-- Error -->
  <div *ngIf="error()" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
    {{ error() }}
  </div>

  <!-- Table -->
  <div *ngIf="!isLoading() && !error() && brands().length > 0" class="bg-white border border-gray-200 rounded-lg overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">ID</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Logo</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Nombre</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Descripción</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Creada</th>
            <th class="px-6 py-3 text-center text-xs font-medium text-gray-700 uppercase">Acciones</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr *ngFor="let brand of brands()" class="hover:bg-gray-50 transition">
            <td class="px-6 py-4 text-sm text-gray-900">{{ brand.id }}</td>
            <td class="px-6 py-4 text-sm">
              <img 
                *ngIf="brand.imageUrl" 
                [src]="brand.imageUrl" 
                [alt]="brand.nombre"
                class="w-12 h-12 object-cover rounded"
              />
              <span *ngIf="!brand.imageUrl" class="text-gray-500 text-xs">Sin logo</span>
            </td>
            <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ brand.nombre }}</td>
            <td class="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
              {{ brand.descripcion || '—' }}
            </td>
            <td class="px-6 py-4 text-sm text-gray-600">
              {{ brand.createdAt ? (brand.createdAt | date:'dd/MM/yyyy') : '—' }}
            </td>
            <td class="px-6 py-4 text-sm text-center space-x-2">
              <a
                [routerLink]="['/admin/brands/edit', brand.id]"
                class="inline-block px-3 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition"
              >
                ✏️ Editar
              </a>
              <button
                (click)="deleteBrand(brand.id, brand.nombre)"
                class="inline-block px-3 py-1 bg-red-100 text-red-700 rounded hover:bg-red-200 transition"
              >
                🗑️ Eliminar
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-between">
      <div class="text-sm text-gray-600">
        Página {{ currentPage() + 1 }} de {{ totalPages() }} ({{ totalElements() }} marcas)
      </div>
      <div class="flex gap-2">
        <button
          (click)="previousPage()"
          [disabled]="currentPage() === 0"
          class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          ← Anterior
        </button>
        <button
          (click)="nextPage()"
          [disabled]="currentPage() >= totalPages() - 1"
          class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Siguiente →
        </button>
      </div>
    </div>
  </div>

  <!-- Empty State -->
  <div *ngIf="!isLoading() && !error() && brands().length === 0" class="bg-white border border-gray-200 rounded-lg p-12 text-center">
    <p class="text-gray-600 text-lg">No se encontraron marcas</p>
  </div>
</div>`,
  styles: []
})
export class BrandsListComponent implements OnInit {
  brands = signal<Brand[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  searchTerm = '';

  private pageSize = 20;
  private deleteTimeoutId: any;

  constructor(private brandService: BrandService) {}

    // Estado para alternar entre activas e inactivas
    showInactive = signal(false);
    inactiveBrands = signal<Brand[]>([]);
    inactiveLoading = signal(false);
    inactiveError = signal<string | null>(null);

    toggleInactive(): void {
      this.showInactive.set(!this.showInactive());
      if (this.showInactive()) {
        this.loadInactiveBrands();
      }
    }

    loadInactiveBrands(page: number = 0): void {
      this.inactiveLoading.set(true);
      this.inactiveError.set(null);
      this.brandService.getInactiveBrands(page, this.pageSize).subscribe({
        next: (response: BrandPage) => {
          this.inactiveBrands.set(response.content);
          this.inactiveLoading.set(false);
        },
        error: (err) => {
          this.inactiveError.set('Error al cargar marcas inactivas: ' + (err?.message || 'Error desconocido'));
          this.inactiveLoading.set(false);
        }
      });
    }

    // Alertas amigables reutilizadas
    showAlert = signal<{ message: string, type: 'success' | 'error' } | null>(null);
    showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);

    deactivateBrand(id: number, nombre: string): void {
      this.showConfirm.set({
        message: `¿Seguro que quieres desactivar la marca "${nombre}"?`,
        onConfirm: () => {
          this.brandService.deleteBrand(id).subscribe({
            next: () => {
              this.loadBrands(this.currentPage());
              this.showAlert.set({ message: 'Marca desactivada correctamente', type: 'success' });
            },
            error: () => {
              this.showAlert.set({ message: 'Error al desactivar marca', type: 'error' });
              this.loadBrands(this.currentPage());
            }
          });
        }
      });
    }

    reactivateBrand(id: number): void {
      this.brandService.reactivateBrand(id).subscribe({
        next: () => {
          this.loadInactiveBrands();
          this.showAlert.set({ message: 'Marca reactivada correctamente', type: 'success' });
        },
        error: () => {
          this.showAlert.set({ message: 'Error al reactivar marca', type: 'error' });
        }
      });
    }

    // Modal flotante para eliminados
    showEliminadosModal = signal(false);
    openEliminadosModal(): void {
      this.showEliminadosModal.set(true);
      this.loadInactiveBrands();
    }
    closeEliminadosModal(): void {
      this.showEliminadosModal.set(false);
      this.loadBrands(this.currentPage());
    }

  ngOnInit(): void {
    this.loadBrands();
  }

  loadBrands(page: number = 0): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.brandService.getBrands(page, this.pageSize).subscribe({
      next: (response: BrandPage) => {
        this.brands.set(response.content);
        this.currentPage.set(response.pageable.pageNumber);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar marcas: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  search(): void {
    if (!this.searchTerm.trim()) {
      this.reset();
      return;
    }

    // Búsqueda local en el cliente
    this.isLoading.set(true);
    this.error.set(null);

    this.brandService.getBrands(0, 1000).subscribe({
      next: (response: BrandPage) => {
        const filtered = response.content.filter(brand =>
          brand.nombre.toLowerCase().includes(this.searchTerm.toLowerCase())
        );
        this.brands.set(filtered);
        this.currentPage.set(0);
        this.totalPages.set(1);
        this.totalElements.set(filtered.length);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al buscar marcas: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  reset(): void {
    this.searchTerm = '';
    this.loadBrands(0);
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadBrands(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.loadBrands(this.currentPage() + 1);
    }
  }

  deleteBrand(id: number, brandName: string): void {
    this.showConfirm.set({
      message: `¿Seguro que quieres eliminar la marca "${brandName}"?`,
      onConfirm: () => {
        // Eliminar visualmente de inmediato
        this.brands.set(this.brands().filter(b => b.id !== id));

        this.brandService.deleteBrand(id).subscribe({
          next: () => {
            this.showAlert.set({ message: `✅ Marca "${brandName}" eliminada`, type: 'success' });
            this.deleteTimeoutId = setTimeout(() => this.showAlert.set(null), 3000);
          },
          error: (err) => {
            // Recargar si falla
            this.loadBrands(this.currentPage());
            this.error.set('Error al eliminar marca: ' + (err.message || 'Error desconocido'));
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.deleteTimeoutId) {
      clearTimeout(this.deleteTimeoutId);
    }
  }
}
