import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from './services/product.service';
import { AuthService } from './services/auth.service';

import { Product } from './models/product.model';
import { HeaderComponent } from './components/header.component';
import { ProductCardComponent } from './components/product-card.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, ProductCardComponent],
  templateUrl: './app.html'
})
export class App implements OnInit {
  private productService = inject(ProductService);
  private authService = inject(AuthService);

  // Signals para gestionar estado
  products = signal<Product[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  isLoggedIn = signal(false);

  ngOnInit() {
    this.isLoggedIn.set(this.authService.isLoggedInSync());
    this.loadProducts();
  }

  logout() {
    this.authService.logout();
    this.isLoggedIn.set(false);
    window.location.reload();
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
}
