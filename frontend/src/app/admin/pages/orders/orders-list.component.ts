import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService, OrderPage, OrderResponseDTO } from '../../../services/order.service';

@Component({
  selector: 'app-admin-orders-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `<div class="space-y-6">
<div class="flex items-center justify-between">
<h1 class="text-3xl font-bold text-gray-900">Gestión de Órdenes</h1>
</div>
<div class="bg-white border border-gray-200 rounded-lg p-4">
<div class="flex gap-4">
<select [(ngModel)]="selectedStatus" (ngModelChange)="onStatusChange()" class="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
<option value="">Todos los estados</option>
<option value="PENDIENTE_PAGO">Pendiente de pago</option>
<option value="PROCESANDO">Procesando</option>
<option value="ENVIADO">Enviado</option>
<option value="ENTREGADO">Entregado</option>
<option value="CANCELADO">Cancelado</option>
<option value="EXPIRADO">Expirado</option>
</select>
<button (click)="reset()" class="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition">Limpiar</button>
</div>
</div>
<div *ngIf="isLoading()" class="text-center py-12"><p class="text-gray-600">Cargando órdenes...</p></div>
<div *ngIf="error()" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">{{ error() }}</div>
<div *ngIf="!isLoading() && !error() && orders().length > 0" class="bg-white border border-gray-200 rounded-lg overflow-hidden">
<table class="w-full">
<thead class="bg-gray-50 border-b border-gray-200">
<tr>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">ID</th>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Usuario</th>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Total</th>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Estado</th>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Pago</th>
<th class="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Fecha</th>
<th class="px-6 py-3 text-center text-xs font-medium text-gray-700 uppercase">Acciones</th>
</tr>
</thead>
<tbody class="divide-y divide-gray-200">
<tr *ngFor="let order of orders()" class="hover:bg-gray-50 transition">
<td class="px-6 py-4 text-sm text-gray-900">{{ order.id }}</td>
<td class="px-6 py-4 text-sm text-gray-600">{{ order.userId }}</td>
<td class="px-6 py-4 text-sm font-medium text-gray-900">{{ order.total | currency }}</td>
<td class="px-6 py-4 text-sm"><span [ngClass]="getStatusClass(order.estado)">{{ order.estado }}</span></td>
<td class="px-6 py-4 text-sm"><span [ngClass]="order.payment ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'">{{ order.payment ? 'Completado' : 'Pendiente' }}</span></td>
<td class="px-6 py-4 text-sm text-gray-600">{{ order.createdAt | date: "dd/MM/yyyy HH:mm" }}</td>
<td class="px-6 py-4 text-sm text-center"><a [routerLink]="['/admin/orders', order.id]" class="inline-block px-3 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition">Ver</a></td>
</tr>
</tbody>
</table>
<div class="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-between">
<div class="text-sm text-gray-600">Página {{ currentPage() + 1 }} de {{ totalPages() }} ({{ totalElements() }} órdenes)</div>
<div class="flex gap-2">
<button (click)="previousPage()" [disabled]="currentPage() === 0" class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition disabled:opacity-50 disabled:cursor-not-allowed">Anterior</button>
<button (click)="nextPage()" [disabled]="currentPage() >= totalPages() - 1" class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition disabled:opacity-50 disabled:cursor-not-allowed">Siguiente</button>
</div>
</div>
</div>
<div *ngIf="!isLoading() && !error() && orders().length === 0" class="bg-white border border-gray-200 rounded-lg p-12 text-center"><p class="text-gray-600 text-lg">No se encontraron órdenes</p></div>
</div>`,
  styles: []
})
export class OrdersListComponent implements OnInit {
  orders = signal<OrderResponseDTO[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  selectedStatus = '';
  private pageSize = 20;

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page: number = 0): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.orderService.getOrders(page, this.pageSize).subscribe({
      next: (response: OrderPage) => {
        this.orders.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar órdenes: ' + (err?.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  onStatusChange(): void {
    this.currentPage.set(0);
    if (this.selectedStatus) {
      this.getOrdersByStatus();
    } else {
      this.loadOrders(0);
    }
  }

  private getOrdersByStatus(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.orderService.getOrdersByStatus(this.selectedStatus, 0, this.pageSize).subscribe({
      next: (response: OrderPage) => {
        this.orders.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al filtrar órdenes: ' + (err?.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  reset(): void {
    this.selectedStatus = '';
    this.loadOrders(0);
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      const newPage = this.currentPage() - 1;
      if (this.selectedStatus) {
        this.loadOrdersPage(newPage);
      } else {
        this.loadOrders(newPage);
      }
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      const newPage = this.currentPage() + 1;
      if (this.selectedStatus) {
        this.loadOrdersPage(newPage);
      } else {
        this.loadOrders(newPage);
      }
    }
  }

  private loadOrdersPage(page: number): void {
    this.isLoading.set(true);
    this.orderService.getOrdersByStatus(this.selectedStatus, page, this.pageSize).subscribe({
      next: (response: OrderPage) => {
        this.orders.set(response.content);
        this.currentPage.set(response.number);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error: ' + (err?.message || 'Error'));
        this.isLoading.set(false);
      }
    });
  }

  getStatusClass(status: string): string {
    const classes = 'px-2 py-1 rounded text-xs font-medium ';
    switch (status) {
      case 'PENDIENTE_PAGO':
        return classes + 'bg-yellow-100 text-yellow-800';
      case 'PROCESANDO':
        return classes + 'bg-blue-100 text-blue-800';
      case 'ENVIADO':
        return classes + 'bg-purple-100 text-purple-800';
      case 'ENTREGADO':
        return classes + 'bg-green-100 text-green-800';
      case 'CANCELADO':
        return classes + 'bg-red-100 text-red-800';
      default:
        return classes + 'bg-gray-100 text-gray-800';
    }
  }
}
