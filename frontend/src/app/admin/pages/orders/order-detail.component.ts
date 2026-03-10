import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService, OrderResponseDTO } from '../../../services/order.service';

@Component({
  selector: 'app-admin-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="space-y-6">
      <div class="flex items-center">
        <a [routerLink]="['/admin/orders']" class="text-blue-600 hover:text-blue-800">Volver</a>
        <h1 class="text-3xl font-bold text-gray-900 ml-4">Orden #{{ orderId }}</h1>
      </div>

      <div *ngIf="isLoading()" class="text-center py-12">
        <p class="text-gray-600">Cargando detalle...</p>
      </div>

      <div *ngIf="error()" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        {{ error() }}
      </div>

      <div *ngIf="!isLoading() && !error() && order()" class="space-y-6">
        <div class="bg-white border border-gray-200 rounded-lg p-6">
          <h2 class="text-lg font-bold text-gray-900 mb-4">Informacion</h2>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <p class="text-sm text-gray-600">ID</p>
              <p class="text-lg font-medium">{{ order()?.id }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-600">Usuario</p>
              <p class="text-lg font-medium">{{ order()?.userId }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-600">Total</p>
              <p class="text-lg font-bold">{{ order()?.total | currency }}</p>
            </div>
            <div>
              <p class="text-sm text-gray-600">Estado</p>
              <p class="text-lg">{{ order()?.estado }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white border border-gray-200 rounded-lg p-6">
          <h2 class="text-lg font-bold text-gray-900 mb-4">Productos</h2>
          <div *ngIf="order()?.items?.length">
            <p *ngFor="let item of order()?.items" class="py-2">{{ item.product.nombre }} x{{ item.cantidad }} = {{ item.product.precio | currency }}</p>
          </div>
          <div *ngIf="!order()?.items?.length">
            <p class="text-gray-600">Sin productos</p>
          </div>
        </div>
      </div>

      <div *ngIf="!isLoading() && !error() && !order()" class="bg-white border border-gray-200 rounded-lg p-12 text-center">
        <p class="text-gray-600 text-lg">Orden no encontrada</p>
      </div>
    </div>
  `,
  styles: []
})
export class OrderDetailComponent implements OnInit {
  order = signal<OrderResponseDTO | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  orderId: number = 0;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.orderId = params['id'];
      this.loadOrder();
    });
  }

  loadOrder(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.orderService.getOrderById(this.orderId).subscribe({
      next: (order: OrderResponseDTO) => {
        this.order.set(order);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error: ' + (err?.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }
}

