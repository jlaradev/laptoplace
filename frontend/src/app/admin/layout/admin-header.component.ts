import { Component, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header class="bg-white border-b border-gray-200 sticky top-0 z-30">
      <div class="px-6 py-4 flex items-center justify-between">
        <!-- Left Side -->
        <div class="flex items-center gap-4">
          <button
            (click)="toggleSidebar.emit()"
            class="p-2 hover:bg-gray-100 rounded-lg transition text-gray-700"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <h1 class="text-xl font-bold text-gray-900">Panel de Administración</h1>
        </div>

        <!-- Right Side -->
        <div class="flex items-center gap-6">
          <!-- User Info -->
          <div class="flex items-center gap-3">
            <div class="text-right hidden sm:block">
              <p class="text-sm font-semibold text-gray-900">{{ userEmail }}</p>
              <p class="text-xs text-gray-500">Administrador</p>
            </div>
            <div class="w-10 h-10 bg-gray-300 rounded-full flex items-center justify-center text-gray-700 font-semibold">
              {{ getUserInitial() }}
            </div>
          </div>

          <!-- Logout Button -->
          <button
            (click)="handleLogout()"
            class="px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg transition"
            title="Cerrar sesión"
          >
            Salir
          </button>
        </div>
      </div>
    </header>
    <!-- Confirmación amigable -->
    <div *ngIf="showConfirm() as confirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
      <div class="bg-white rounded-lg shadow-lg p-6 max-w-md w-full relative">
        <button class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold" (click)="showConfirm.set(null)">×</button>
        <div class="text-lg font-medium mb-4">{{ confirm.message }}</div>
        <div class="flex justify-end gap-2">
          <button class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition" (click)="showConfirm.set(null)">Cancelar</button>
          <button class="px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition" (click)="confirm.onConfirm(); showConfirm.set(null)">Confirmar</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AdminHeaderComponent {
  @Output() toggleSidebar = new EventEmitter<void>();
  private authService = inject(AuthService);
  private router = inject(Router);
  showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);

  userEmail = this.authService.getUserEmail() || 'Admin';

  getUserInitial(): string {
    return this.userEmail?.charAt(0).toUpperCase() || 'A';
  }

  handleLogout(): void {
    this.showConfirm.set({
      message: '¿Seguro que deseas cerrar sesión?',
      onConfirm: () => {
        this.authService.logout();
        const returnUrl = this.router.url || '/admin/dashboard';
        this.router.navigate(['/admin/login'], { queryParams: { returnUrl } });
      }
    });
  }
}
