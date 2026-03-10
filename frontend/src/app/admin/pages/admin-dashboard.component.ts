import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="space-y-8">
      <!-- Page Title -->
      <div>
        <h1 class="text-4xl font-bold text-gray-900">¡Bienvenido al Panel Admin!</h1>
        <p class="text-gray-600 mt-2">Gestiona tu tienda de laptops desde aquí</p>
      </div>

      <!-- Quick Access -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Productos Section -->
        <div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <span class="text-3xl">📦</span>
            <h2 class="text-xl font-bold text-gray-900">Inventario</h2>
          </div>
          <div class="space-y-3">
            <p class="text-gray-600 text-sm">Gestiona tu catálogo de productos y especificaciones técnicas</p>
            <div class="flex gap-3 pt-4">
              <a
                routerLink="/admin/products"
                class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium text-sm hover:bg-blue-700 transition text-center"
              >
                Ver Productos
              </a>
              <a
                routerLink="/admin/products/create"
                class="flex-1 px-4 py-2 bg-gray-100 text-gray-900 rounded-lg font-medium text-sm hover:bg-gray-200 transition text-center"
              >
                ➕ Nuevo
              </a>
            </div>
          </div>
        </div>

        <!-- Órdenes Section -->
        <div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <span class="text-3xl">📋</span>
            <h2 class="text-xl font-bold text-gray-900">Pedidos</h2>
          </div>
          <div class="space-y-3">
            <p class="text-gray-600 text-sm">Controla el estado de órdenes y gestiona envíos</p>
            <div class="flex gap-3 pt-4">
              <a
                routerLink="/admin/orders"
                class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium text-sm hover:bg-blue-700 transition text-center"
              >
                Ver Órdenes
              </a>

            </div>
          </div>
        </div>

        <!-- Usuarios Section -->
        <div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <span class="text-3xl">👥</span>
            <h2 class="text-xl font-bold text-gray-900">Usuarios</h2>
          </div>
          <div class="space-y-3">
            <p class="text-gray-600 text-sm">Monitorea clientes y datos de compras</p>
            <div class="flex gap-3 pt-4">
              <a
                routerLink="/admin/users"
                class="w-full px-4 py-2 bg-blue-600 text-white rounded-lg font-medium text-sm hover:bg-blue-700 transition text-center"
              >
                Ver Usuarios
              </a>
            </div>
          </div>
        </div>

        <!-- Marcas & Reseñas Section -->
        <div class="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
          <div class="flex items-center gap-3 mb-6">
            <span class="text-3xl">🏷️</span>
            <h2 class="text-xl font-bold text-gray-900">Marcas</h2>
          </div>
          <div class="space-y-3">
            <p class="text-gray-600 text-sm">Gestiona las marcas del catálogo de productos</p>
            <div class="flex gap-3 pt-4">
              <a
                routerLink="/admin/brands"
                class="flex-1 px-4 py-2 bg-gray-100 text-gray-900 rounded-lg font-medium text-sm hover:bg-gray-200 transition text-center"
              >
                Marcas
              </a>

            </div>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: []
})
export class AdminDashboardComponent {
}
