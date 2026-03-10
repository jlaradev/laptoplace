import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="w-64 bg-gray-900 text-white transition-all duration-300 flex flex-col min-h-screen">
      <!-- Logo -->
      <div class="p-6 border-b border-gray-800">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center font-bold">LP</div>
          <div class="flex flex-col">
            <span class="text-sm font-bold">LaptoPlace</span>
            <span class="text-xs text-gray-400">Admin Panel</span>
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
        <!-- Dashboard -->
        <a
          routerLink="/admin/dashboard"
          routerLinkActive="bg-blue-600"
          [routerLinkActiveOptions]="{ exact: true }"
          class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition group"
        >
          <span class="text-lg">📊</span>
          <span class="text-sm font-medium">Dashboard</span>
        </a>

        <!-- Productos -->
        <div class="pt-4">
          <div class="px-4 py-2 text-xs uppercase font-semibold text-gray-500 tracking-wider">
            Catálogo
          </div>
          <a
            routerLink="/admin/products"
            routerLinkActive="bg-blue-600"
            class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition"
          >
            <span class="text-lg">📦</span>
            <span class="text-sm font-medium">Productos</span>
          </a>
          <a
            routerLink="/admin/brands"
            routerLinkActive="bg-blue-600"
            class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition"
          >
            <span class="text-lg">🏷️</span>
            <span class="text-sm font-medium">Marcas</span>
          </a>
        </div>

        <!-- Ventas -->
        <div class="pt-4">
          <div class="px-4 py-2 text-xs uppercase font-semibold text-gray-500 tracking-wider">
            Ventas
          </div>
          <a
            routerLink="/admin/orders"
            routerLinkActive="bg-blue-600"
            class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition"
          >
            <span class="text-lg">📋</span>
            <span class="text-sm font-medium">Órdenes</span>
          </a>

        </div>

        <!-- Comunidad -->
        <div class="pt-4">
          <div class="px-4 py-2 text-xs uppercase font-semibold text-gray-500 tracking-wider">
            Comunidad
          </div>
          <a
            routerLink="/admin/users"
            routerLinkActive="bg-blue-600"
            class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-gray-800 transition"
          >
            <span class="text-lg">👥</span>
            <span class="text-sm font-medium">Usuarios</span>
          </a>

        </div>
      </nav>

      <!-- Footer -->
      <div class="border-t border-gray-800 p-4">
        <p class="text-xs text-gray-500 text-center">
          LaptoPlace Admin v1.0
        </p>
      </div>
    </aside>
  `,
  styles: []
})
export class AdminSidebarComponent {
  @Input() collapsed = false;
}
