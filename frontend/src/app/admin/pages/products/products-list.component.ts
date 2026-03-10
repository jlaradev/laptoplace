import { Component, OnInit, signal } from '@angular/core';
import { AdminComponentsModule } from '../../components/admin-components.module';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { Product, ProductPage } from '../../../models/product.model';

@Component({
  selector: 'app-admin-products-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, AdminComponentsModule],
  templateUrl: './products-list.component.html',
  styles: []
})
export class ProductsListComponent implements OnInit {
  products = signal<Product[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  searchTerm = '';

  private pageSize = 20;

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

    // Estado para alternar entre activos e inactivos
    showInactive = signal(false);
    inactiveProducts = signal<Product[]>([]);
    inactiveLoading = signal(false);
    inactiveError = signal<string | null>(null);

    toggleInactive(): void {
      this.showInactive.set(!this.showInactive());
      if (this.showInactive()) {
        this.loadInactiveProducts();
      }
    }

    loadInactiveProducts(page: number = 0): void {
      this.inactiveLoading.set(true);
      this.inactiveError.set(null);
      this.productService.getInactiveProducts(page, this.pageSize).subscribe({
        next: (response) => {
          this.inactiveProducts.set(response.content);
          this.inactiveLoading.set(false);
        },
        error: (err) => {
          this.inactiveError.set('Error al cargar productos inactivos: ' + (err?.message || 'Error desconocido'));
          this.inactiveLoading.set(false);
        }
      });
    }

    // Alertas amigables reutilizadas
    showAlert = signal<{ message: string, type: 'success' | 'error' } | null>(null);
    showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);

    deactivateProduct(id: number, nombre: string): void {
      this.showConfirm.set({
        message: `¿Seguro que quieres desactivar el producto "${nombre}"?`,
        onConfirm: () => {
          this.productService.deleteProduct(id).subscribe({
            next: () => {
              this.loadProducts(this.currentPage());
              this.showAlert.set({ message: 'Producto desactivado correctamente', type: 'success' });
            },
            error: () => {
              this.showAlert.set({ message: 'Error al desactivar producto', type: 'error' });
            }
          });
        }
      });
    }

    reactivateProduct(id: number): void {
      this.productService.reactivateProduct(id).subscribe({
        next: () => {
          this.loadInactiveProducts();
          this.showAlert.set({ message: 'Producto reactivado correctamente', type: 'success' });
        },
        error: () => {
          this.showAlert.set({ message: 'Error al reactivar producto', type: 'error' });
        }
      });
    }

    // Modal flotante para eliminados
    showEliminadosModal = signal(false);
    openEliminadosModal(): void {
      this.showEliminadosModal.set(true);
      this.loadInactiveProducts();
    }
    closeEliminadosModal(): void {
      this.showEliminadosModal.set(false);
      this.loadProducts(this.currentPage());
    }

  loadProducts(page: number = 0): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.productService.getProducts(page, this.pageSize).subscribe({
      next: (response) => {
        this.products.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar productos: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  search(): void {
    if (!this.searchTerm.trim()) {
      this.reset();
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    this.productService.searchByName(this.searchTerm, 0, this.pageSize).subscribe({
      next: (response) => {
        this.products.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al buscar productos: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  reset(): void {
    this.searchTerm = '';
    this.loadProducts(0);
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadProducts(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.loadProducts(this.currentPage() + 1);
    }
  }

  // Eliminados duplicados

  formatPrice(price: number): string {
    return price.toFixed(2);
  }

  formatRating(rating: number): string {
    return rating.toFixed(1);
  }
}
