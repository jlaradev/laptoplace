import { Component, signal, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { ProductCardComponent } from '../components/product-card.component';
import { ProductService } from '../services/product.service';
import { BrandService } from '../services/brand.service';
import { AuthService } from '../services/auth.service';
import { Product } from '../models/product.model';
import { Brand } from '../models/brand.model';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent, ProductCardComponent],
  template: `
    <div class="flex flex-col min-h-screen h-screen bg-white">
      <!-- Header -->
      <app-header></app-header>

      <main class="flex-1 flex bg-white">
        <!-- Sidebar (hidden on mobile) -->
        <aside class="hidden sm:block w-80 border-r border-slate-200 p-8">
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
          <select 
            [(ngModel)]="selectedBrandId"
            (change)="onBrandChange()"
            name="brand"
            class="w-full px-3 py-2 border border-slate-300 rounded-lg font-medium bg-white">
            <option value="">Todas las marcas</option>
            <option *ngFor="let brand of allBrands()" [value]="brand.id">
              {{ brand.nombre }}
            </option>
          </select>
        </aside>
        <!-- Main Content -->
        <section class="flex-1 p-6 min-h-screen">
          <!-- Mobile: toggle filters -->
          <div class="mb-4 sm:hidden">
            <button (click)="toggleMobileFilters()" class="w-full flex items-center justify-between px-4 py-3 bg-slate-100 rounded-lg border">
              <span class="font-medium">Mostrar filtros</span>
              <svg *ngIf="!showFiltersMobile()" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              <svg *ngIf="showFiltersMobile()" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
              </svg>
            </button>

            <div *ngIf="showFiltersMobile()" class="mt-3 p-4 bg-white border rounded-lg">
              <label class="block text-sm font-semibold mb-2">Buscar producto</label>
              <form (ngSubmit)="onSearch()" class="flex gap-2 mb-3">
                <input type="text" [(ngModel)]="search" name="searchMobile" placeholder="Buscar..." class="w-full px-3 py-2 border rounded-lg" />
                <button type="submit" class="px-3 py-2 bg-blue-600 text-white rounded-lg">Ir</button>
              </form>
              <button (click)="clearFilters()" class="mb-3 px-4 py-2 bg-slate-200 text-slate-700 rounded-lg font-semibold hover:bg-slate-300 transition w-full">Limpiar filtros</button>
              <h2 class="text-md font-bold mb-2">Filtrar por marca</h2>
              <select 
                [(ngModel)]="selectedBrandId"
                (change)="onBrandChange()"
                name="brandMobile"
                class="w-full px-3 py-2 border border-slate-300 rounded-lg font-medium bg-white">
                <option value="">Todas las marcas</option>
                <option *ngFor="let brand of allBrands()" [value]="brand.id">
                  {{ brand.nombre }}
                </option>
              </select>
            </div>
          </div>
          <!-- Saludo movido al header -->
          <h1 class="text-2xl font-bold mb-6">Catálogo completo</h1>
          
          <!-- Sort Dropdown -->
          <div class="mb-6">
            <label class="block text-sm font-semibold mb-2">Ordenar por:</label>
            <select 
              (change)="onSortSelectChange($event)"
              [value]="0"
              class="w-full px-4 py-2 border border-slate-300 rounded-lg font-medium bg-white hover:border-slate-400 transition">
              <option *ngFor="let option of sortOptions; let i = index" [value]="i">
                {{ option.label }}
              </option>
            </select>
          </div>

          <!-- Loader centrado solo si no hay productos -->
          <div *ngIf="loading() && products().length === 0" class="flex flex-col items-center justify-center w-full min-h-40 mb-8">
            <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
            <span class="text-blue-700 font-semibold text-lg">Cargando productos...</span>
          </div>
          <ng-container *ngIf="products().length > 0">
            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <app-product-card *ngFor="let product of products()" [product]="product"></app-product-card>
            </div>
            <!-- Loader pequeño debajo de productos al paginar -->
            <div *ngIf="loading() && products().length > 0" class="flex flex-col items-center justify-center w-full min-h-20 my-8">
              <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-2"></div>
              <span class="text-blue-700 font-semibold text-base">Cargando más productos...</span>
            </div>
          </ng-container>
          <div *ngIf="!loading() && products().length === 0" class="col-span-full text-center py-12 w-full">
            <p class="text-gray-500 text-lg">No se encontraron productos que coincidan con tu búsqueda</p>
          </div>
          <div class="flex justify-center mt-8" *ngIf="!isLastPage && !loading() && products().length > 0">
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
  private brandService = inject(BrandService);
  products = signal<Product[]>([]);
  allBrands = signal<Brand[]>([]);
  loading = signal(false);
  page = 0;
  isLastPage = false;
  isLoggedIn = signal(false);
  private apiSub: Subscription | null = null;
  private authService = inject(AuthService);
  sortBy = signal<string>('createdAt');
  sortDirection = signal<string>('desc');
  selectedBrandId: string | number | null = "";
  sortOptions = [
    { value: 'createdAt', label: 'Más nuevos', direction: 'desc' },
    { value: 'rating', label: 'Mejor valorados', direction: 'desc' },
    { value: 'price', label: 'Precio: menor a mayor', direction: 'asc' },
    { value: 'price', label: 'Precio: mayor a menor', direction: 'desc' },
    { value: 'name', label: 'Alfabéticamente A-Z', direction: 'asc' },
    { value: 'name', label: 'Alfabéticamente Z-A', direction: 'desc' }
  ];
  search = '';
  searchTerm = '';
  userEmail: string | null = null;
  showFiltersMobile = signal(false);

  constructor() {
    this.isLoggedIn.set(this.authService.isLoggedInSync());
    this.userEmail = this.authService.getUserEmail();

    // Cargar marcas de la BD
    this.brandService.getAllBrands().subscribe({
      next: (brands) => {
        this.allBrands.set(brands);
      },
      error: (err) => {
      }
    });

    // Leer query param 'brandId' y seleccionar la marca si existe
    const route = inject(ActivatedRoute);
    route.queryParams.subscribe(params => {
      const brandIdParam = params['brandId'];
      if (brandIdParam) {
        this.selectedBrandId = parseInt(brandIdParam);
      }
      this.loadProducts(true);
    });
  }
  logout() {
    this.authService.logout();
    this.isLoggedIn.set(false);
    window.location.reload();
  }

  onSearch() {
    this.searchTerm = this.search.trim();
    this.page = 0;
    this.products.set([]);
    this.isLastPage = false;
    this.loadProducts(true);
  }

  onSortChange(option: { value: string; label: string; direction: string }) {
    this.sortBy.set(option.value);
    this.sortDirection.set(option.direction);
    this.page = 0;
    this.products.set([]);
    this.isLastPage = false;
    this.loadProducts(true);
  }

  onSortSelectChange(event: any) {
    const index = parseInt(event.target.value);
    const option = this.sortOptions[index];
    this.onSortChange(option);
  }

  clearFilters() {
    this.search = '';
    this.searchTerm = '';
    this.selectedBrandId = "";
    this.sortBy.set('createdAt');
    this.sortDirection.set('desc');
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
    
    const search = this.searchTerm.trim();
    const pageSize = 20;

    // Construir opciones de búsqueda dinámicamente
    const searchOptions: any = {
      sortBy: this.sortBy(),
      sort: this.sortDirection(),
      page: this.page,
      size: pageSize
    };

    if (search) {
      searchOptions.nombre = search;
    }

    if (this.selectedBrandId && this.selectedBrandId !== "") {
      searchOptions.brandId = Number(this.selectedBrandId);
    }

    // Usar el nuevo método search unificado
    this.apiSub = this.productService.search(searchOptions).subscribe(page => {
      let content = page.content;
      
      if (reset) {
        this.products.set(content);
      } else {
        this.products.set([...this.products(), ...content]);
      }
      this.isLastPage = (page.number + 1) >= page.totalPages || content.length === 0;
      this.loading.set(false);
    });
  }

  toggleMobileFilters() {
    this.showFiltersMobile.update(v => !v);
  }
}
