import { Component, OnInit, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from './services/product.service';
import { BrandService } from './services/brand.service';
import { AuthService } from './services/auth.service';

import { Product } from './models/product.model';
import { Brand } from './models/brand.model';
import { HeaderComponent } from './components/header.component';
import { ProductCardComponent } from './components/product-card.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, ProductCardComponent],
  templateUrl: './app.html'
})
export class App implements OnInit {
  @ViewChild('brandCarousel') brandCarousel?: ElementRef<HTMLDivElement>;

  private productService = inject(ProductService);
  private brandService = inject(BrandService);
  private authService = inject(AuthService);

  // Signals para gestionar estado
  products = signal<Product[]>([]);
  brands = signal<Brand[]>([]);
  brandsJson = signal<string>('');
  topRatedProducts = signal<Product[]>([]);
  loading = signal(true);
  loadingTopRated = signal(true);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  isLoggedIn = signal(false);
  brandCarouselIndex = signal(0);
  displayBrands = signal<Brand[]>([]);

  ngOnInit() {
    this.isLoggedIn.set(this.authService.isLoggedInSync());
    this.loadProducts();
    this.loadBrands();
    this.loadTopRatedProducts();
  }

  logout() {
    this.authService.logout();
    this.isLoggedIn.set(false);
    window.location.reload();
  }

  loadBrands() {
    this.brandService.getAllBrands().subscribe({
      next: (brands) => {
        this.brands.set(brands);
        this.displayBrands.set(brands);
        this.brandsJson.set(JSON.stringify(brands, null, 2));
        this.brandCarouselIndex.set(0);
      },
      error: (err) => {
        console.error('Error loading brands:', err);
      }
    });
  }

  loadTopRatedProducts() {
    this.loadingTopRated.set(true);
    this.productService.getTopRatedProducts().subscribe({
      next: (response) => {
        this.topRatedProducts.set(response.content);
        this.loadingTopRated.set(false);
      },
      error: (err) => {
        console.error('Error loading top rated products:', err);
        this.loadingTopRated.set(false);
      }
    });
  }

  loadProducts() {
    this.loading.set(true);
    this.error.set(null);

    this.productService.getProducts(this.currentPage(), 20).subscribe({
      next: (response) => {
        this.products.set(response.content);
        this.totalPages.set(response.totalPages ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading products:', err);
        this.error.set('No se pudieron cargar los productos. Intenta de nuevo.');
        this.loading.set(false);
      }
    });
  }

  nextPage() {
    if (this.currentPage() + 1 < this.totalPages()) {
      this.currentPage.update(page => page + 1);
      this.loadProducts();
    }
  }

  previousPage() {
    if (this.currentPage() > 0) {
      this.currentPage.update(page => page - 1);
      this.loadProducts();
    }
  }

  handleCarouselScroll() {
    // Método ya no es necesario
  }

  scrollBrandCarousel(direction: 'left' | 'right') {
    if (!this.brandCarousel?.nativeElement) return;

    const carousel = this.brandCarousel.nativeElement;
    const itemWidth = (carousel.clientWidth * 0.235) + 24;
    const itemsVisible = Math.floor(carousel.clientWidth / itemWidth);
    const totalBrands = this.brands().length;
    let currentIndex = this.brandCarouselIndex();

    if (direction === 'right') {
      // Mover por el número de items visibles
      currentIndex = currentIndex + itemsVisible;
      
      // Si se pasa del final, volver al principio
      if (currentIndex >= totalBrands) {
        currentIndex = 0;
      }
    } else {
      // Mover hacia atrás
      currentIndex = currentIndex - itemsVisible;
      
      // Si va antes del inicio, ir al final
      if (currentIndex < 0) {
        currentIndex = Math.max(0, totalBrands - itemsVisible);
      }
    }

    this.brandCarouselIndex.set(currentIndex);
    this.scrollToIndex(currentIndex);
  }

  private scrollToIndex(index: number) {
    if (!this.brandCarousel?.nativeElement) return;

    const carousel = this.brandCarousel.nativeElement;
    const itemWidth = (carousel.clientWidth * 0.235) + 24;
    const scrollPosition = itemWidth * index;

    carousel.scrollBy({ left: scrollPosition - carousel.scrollLeft, behavior: 'smooth' });
  }
}
