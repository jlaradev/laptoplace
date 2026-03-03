import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { OrderService, OrderResponseDTO, OrderPage } from '../services/order.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, RouterLink],
  template: `
    <div class="flex flex-col min-h-screen bg-gray-50">
      <!-- Header -->
      <app-header></app-header>

      <!-- Main Content -->
      <main class="flex-1 max-w-6xl mx-auto w-full px-4 py-8">
        <h1 class="text-3xl font-bold mb-8">Mis Pedidos</h1>

        <!-- Loading State (solo si no hay órdenes aún) -->
        <div *ngIf="loading() && orders().length === 0" class="flex flex-col items-center justify-center w-full min-h-64">
          <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
          <span class="text-blue-700 font-semibold text-lg">Cargando tus pedidos...</span>
        </div>

        <!-- Empty State -->
        <div *ngIf="!loading() && orders().length === 0" class="text-center py-12">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
          </svg>
          <p class="text-gray-500 text-lg mb-4">No tienes pedidos aún</p>
          <a routerLink="/catalogo" class="text-blue-600 font-semibold hover:underline">Ir al catálogo</a>
        </div>

        <!-- Orders List -->
        <div *ngIf="orders().length > 0" class="space-y-6">
          <div *ngFor="let order of orders()" class="bg-white rounded-lg shadow-md p-6 border-l-4 border-blue-600">
            <!-- Order Header -->
            <div class="flex flex-col md:flex-row md:justify-between md:items-center mb-4 pb-4 border-b border-gray-200">
              <div>
                <p class="text-xs text-gray-500">{{ formatDate(order.createdAt) }}</p>
              </div>
              <div class="mt-3 md:mt-0 flex items-center gap-4">
                <span class="px-3 py-1 rounded-full text-sm font-semibold" [ngClass]="getStatusClass(order.estado)">
                  {{ getStatusLabel(order.estado) }}
                </span>
                <p class="text-xl font-bold text-gray-900">$ {{ order.total | number:'1.2-2' }}</p>
              </div>
            </div>

            <!-- Order Items -->
            <div class="mb-4">
              <h3 class="font-semibold text-gray-700 mb-3">Artículos:</h3>
              <div class="space-y-2">
                <div *ngFor="let item of order.items" class="flex justify-between text-sm text-gray-700 bg-gray-50 p-3 rounded">
                  <span>{{ item.product.nombre }} x{{ item.cantidad }}</span>
                  <span>$ {{ (item.precioUnitario * item.cantidad | number:'1.2-2') }}</span>
                </div>
              </div>
            </div>

            <!-- Order Details -->
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm text-gray-600"><strong>Dirección de envío:</strong> {{ order.direccionEnvio }}</p>
            </div>
          </div>

          <!-- Loader pequeño al cargar más -->
          <div *ngIf="loading()" class="flex flex-col items-center justify-center w-full min-h-20 my-8">
            <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-2"></div>
            <span class="text-blue-700 font-semibold text-base">Cargando más pedidos...</span>
          </div>

          <!-- Load More Button -->
          <div class="flex justify-center mt-8" *ngIf="!isLastPage && !loading()">
            <button (click)="loadMore()" class="px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed">
              Cargar más
            </button>
          </div>
        </div>
      </main>

      <!-- Footer -->
      <app-footer></app-footer>
    </div>
  `
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);
  private authService = inject(AuthService);

  orders = signal<OrderResponseDTO[]>([]);
  loading = signal(false);
  pageSize = 85;
  page = 0;
  totalPages = 0;
  isLastPage = false;
  userId: string | null = null;

  ngOnInit() {
    this.userId = localStorage.getItem('userId');
    if (!this.userId) {
      console.error('No user ID found');
      return;
    }
    this.loadOrders(true);
  }

  loadOrders(reset: boolean = false) {
    if (!this.userId) return;
    
    if (reset) {
      this.page = 0;
      this.orders.set([]);
      this.isLastPage = false;
    }
    
    this.loading.set(true);
    this.orderService.getUserOrders(this.userId, this.page, this.pageSize).subscribe({
      next: (response: OrderPage) => {
        this.totalPages = response.totalPages ?? 0;
        this.isLastPage = this.page >= (this.totalPages - 1);
        
        // Filtrar solo órdenes en proceso, enviadas o entregadas
        const filteredOrders = response.content.filter(order => {
          const status = order.estado.toUpperCase();
          return status === 'PROCESANDO' || status === 'ENVIADO' || status === 'ENTREGADO';
        });
        
        if (reset) {
          this.orders.set(filteredOrders);
        } else {
          this.orders.update(orders => [...orders, ...filteredOrders]);
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading orders:', err);
        this.loading.set(false);
      }
    });
  }

  loadMore() {
    this.page++;
    this.loadOrders(false);
  }

  getStatusLabel(status: string): string {
    const statusMap: { [key: string]: string } = {
      'procesando': 'En Proceso',
      'enviado': 'Enviado',
      'entregado': 'Entregado',
      'pendiente_pago': 'Pendiente de Pago',
      'cancelado': 'Cancelado',
      'expirado': 'Expirado'
    };
    return statusMap[status.toLowerCase()] || status;
  }

  getStatusClass(status: string): string {
    const statusLower = status.toLowerCase();
    if (statusLower === 'procesando') {
      return 'bg-yellow-100 text-yellow-800';
    } else if (statusLower === 'enviado') {
      return 'bg-orange-100 text-orange-800';
    } else if (statusLower === 'entregado') {
      return 'bg-green-100 text-green-800';
    }
    return 'bg-gray-100 text-gray-800';
  }

  formatDate(dateString: string | null | undefined): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
