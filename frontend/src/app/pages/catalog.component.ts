import { Component, signal, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { ProductCardComponent } from '../components/product-card.component';
import { ProductService } from '../services/product.service';
import { AuthService } from '../services/auth.service';
import { Product } from '../models/product.model';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent, ProductCardComponent],
  template: `
    <div class="flex flex-col min-h-screen h-screen bg-white">
      <!-- Header -->
      <app-header></app-header>

      <main class="flex-1 flex bg-white">
        <!-- Sidebar -->
        <aside class="w-80 border-r border-slate-200 p-8">
          <label class="block text-sm font-semibold mb-2">Buscar producto</label>
          <form (ngSubmit)="onSearch()" class="flex gap-2 mb-4">
            <input type="text" [(ngModel)]="search" name="search" placeholder="Buscar..." class="w-full px-3 py-2 border rounded-lg" />
            <button type="submit" class="px-4 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition flex items-center justify-center" aria-label="Buscar">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 104.5 4.5a7.5 7.5 0 0012.15 12.15z" />
              </svg>
            </button>
          </form>
          <button (click)="clearFilters()" class="mb-6 px-4 py-2 bg-slate-200 text-slate-700 rounded-lg font-semibold hover:bg-slate-300 transition w-full">Limpiar filtros</button>
          <h2 class="text-lg font-bold mb-4">Filtrar por marca</h2>
          <div *ngFor="let brand of brands">
            <label class="flex items-center gap-2 mb-2">
              <input type="checkbox" [(ngModel)]="brand.selected" (change)="onBrandChange()" />
              <span>{{ brand.name }}</span>
            </label>
          </div>
        </aside>
        <!-- Main Content -->
        <section class="flex-1 p-6 min-h-screen">
          <!-- Saludo movido al header -->
          <h1 class="text-2xl font-bold mb-6">Catálogo completo</h1>
          <!-- Loader centrado solo si no hay productos -->
          <div *ngIf="loading() && filteredProducts().length === 0" class="flex flex-col items-center justify-center w-full min-h-40 mb-8">
            <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
            <span class="text-blue-700 font-semibold text-lg">Cargando productos...</span>
          </div>
          <ng-container *ngIf="filteredProducts().length > 0">
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <app-product-card *ngFor="let product of filteredProducts()" [product]="product"></app-product-card>
            </div>
            <!-- Loader pequeño debajo de productos al paginar -->
            <div *ngIf="loading() && filteredProducts().length > 0" class="flex flex-col items-center justify-center w-full min-h-20 my-8">
              <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-2"></div>
              <span class="text-blue-700 font-semibold text-base">Cargando más productos...</span>
            </div>
          </ng-container>
          <div class="flex justify-center mt-8" *ngIf="!isLastPage && !loading() && filteredProducts().length > 0">
            <button (click)="loadMore()" class="px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed">
              Cargar más
            </button>
          </div>
        </section>
      </main>

      <app-footer></app-footer>
    <!-- Div invisible para forzar scroll solo en catálogo -->
    </div>
  `,
  styles: []
})
export class CatalogComponent {
  private productService = inject(ProductService);
  products = signal<Product[]>([]);
  loading = signal(false);
  page = 0;
  isLastPage = false;
  isLoggedIn = signal(false);
  private apiSub: Subscription | null = null;
  private authService = inject(AuthService);
  brands = [
    { name: 'HP', selected: false },
    { name: 'Dell', selected: false },
    { name: 'Lenovo', selected: false },
    { name: 'ASUS', selected: false },
    { name: 'Acer', selected: false },
    { name: 'Apple', selected: false },
    { name: 'MSI', selected: false },
    { name: 'Samsung', selected: false }
  ];
  search = '';
  searchTerm = '';
  userEmail: string | null = null;

  constructor() {
    this.isLoggedIn.set(this.authService.isLoggedInSync());
    this.userEmail = this.authService.getUserEmail();

    // Leer query param 'brand' y seleccionar la marca si existe
    const route = inject(ActivatedRoute);
    route.queryParams.subscribe(params => {
      const brandParam = params['brand'];
      if (brandParam) {
        this.brands.forEach(b => b.selected = b.name.toLowerCase() === brandParam.toLowerCase());
      }
      this.loadProducts(true);
    });
  }
  logout() {
    this.authService.logout();
    this.isLoggedIn.set(false);
    window.location.reload();
  }

  get selectedBrand(): string | null {
    const selected = this.brands.find(b => b.selected);
    return selected ? selected.name : null;
  }

  onSearch() {
    this.searchTerm = this.search.trim();
    this.page = 0;
    this.products.set([]);
    this.isLastPage = false;
    this.loadProducts(true);
  }

  clearFilters() {
    this.search = '';
    this.searchTerm = '';
    this.brands.forEach(b => b.selected = false);
    this.page = 0;
    this.products.set([]);
    this.isLastPage = false;
    this.loadProducts(true);
  }

  onBrandChange() {
    this.page = 0;
    this.products.set([]);
    this.isLastPage = false;
    this.loadProducts(true);
  }

  loadMore() {
    this.page++;
    this.loadProducts();
  }

  loadProducts(reset: boolean = false) {
    this.loading.set(true);
    // Cancelar la suscripción anterior si existe
    if (this.apiSub) {
      this.apiSub.unsubscribe();
    }
    let obs;
    const selectedBrands = this.brands.filter(b => b.selected).map(b => b.name);
    const hasBrandFilter = selectedBrands.length > 0;
    const search = this.searchTerm.trim();
    const pageSize = 20;
    if (search && selectedBrands.length === 1) {
      // Buscar por nombre y una marca: traer todos por nombre y filtrar por marca en frontend (no hay endpoint combinado)
      obs = this.productService.searchByName(search, this.page, pageSize);
    } else if (search) {
      // Buscar por nombre y varias marcas: traer por nombre y filtrar en frontend
      obs = this.productService.searchByName(search, this.page, pageSize);
    } else if (selectedBrands.length === 1) {
      // Solo una marca: usar endpoint backend con paginación real
      obs = this.productService.findByBrand(selectedBrands[0], this.page, pageSize);
    } else {
      // Sin marcas o varias marcas: traer todos y filtrar en frontend
      obs = this.productService.getProducts(this.page, pageSize);
    }
    this.apiSub = obs.subscribe(page => {
      let content = page.content;
      const selectedBrandsLower = this.brands.filter(b => b.selected).map(b => b.name.toLowerCase());
      // Si hay más de una marca seleccionada, filtrar en frontend
      if (selectedBrandsLower.length > 1) {
        content = content.filter(p => selectedBrandsLower.includes((p.marca || '').toLowerCase()));
      }
      if (reset) {
        this.products.set(content);
      } else {
        this.products.set([...this.products(), ...content]);
      }
      this.isLastPage = (page.number + 1) >= page.totalPages || content.length === 0;
      this.loading.set(false);
    });
  }

  updateFilters() {
    // Trigger Angular change detection
    this.products.set([...this.products()]);
  }

  filteredProducts() {
    let filtered = this.products();
    const selectedBrands = this.brands.filter(b => b.selected).map(b => b.name.toLowerCase());
    const hasBrandFilter = selectedBrands.length > 0;
    const hasSearch = this.searchTerm.length > 0;
    const searchTerm = this.searchTerm.toLowerCase();

    // Filtrar por marca SIEMPRE que se seleccione una marca
    if (hasBrandFilter) {
      filtered = filtered.filter(p => selectedBrands.includes((p.marca || '').toLowerCase()));
    }
    // Filtrar por nombre SOLO si se ha pulsado Buscar
    if (hasSearch) {
      filtered = filtered.filter(p => (p.nombre || '').toLowerCase().includes(searchTerm));
    }
    return filtered;
  }

}
