import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="flex flex-col min-h-screen bg-gray-50">
      <!-- Header -->
      <header class="border-b border-gray-300 sticky top-0 bg-white z-40">
        <div class="max-w-[1440px] mx-auto px-4 md:px-6 py-5 flex items-center justify-between">
          <a routerLink="/" class="flex items-center text-xl font-bold text-gray-900">
            Lapto<span class="text-gray-700">Place</span> <span class="text-xs ml-2 bg-gray-900 text-white px-2 py-1 rounded">Admin</span>
          </a>
        </div>
      </header>

      <!-- Main Content -->
      <main class="flex-1 flex items-center justify-center px-4 py-12">
        <div class="w-full max-w-md">
          <div class="bg-white border border-gray-300 rounded-lg p-8 shadow-sm">
            <div class="mb-8">
              <h1 class="text-3xl font-bold text-gray-900 mb-2">Portal Administrativo</h1>
              <p class="text-gray-600">Acceso restringido a administradores</p>
            </div>

            <!-- Error Message -->
            <div *ngIf="error()" class="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-red-700 text-sm font-semibold">{{ error() }}</p>
            </div>

            <!-- Warning Message -->
            <div *ngIf="isUserNotAdmin()" class="mb-6 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p class="text-yellow-700 text-sm">Tu cuenta no tiene permisos de administrador</p>
            </div>

            <!-- Login Form -->
            <form (ngSubmit)="handleLogin()" class="space-y-4">
              <!-- Email Input -->
              <div>
                <label class="block text-sm font-semibold text-gray-900 mb-2">Correo</label>
                <input
                  type="email"
                  [(ngModel)]="email"
                  name="email"
                  placeholder="admin@ejemplo.com"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 transition"
                  [disabled]="loading()"
                />
              </div>

              <!-- Password Input -->
              <div>
                <label class="block text-sm font-semibold text-gray-900 mb-2">Contraseña</label>
                <input
                  type="password"
                  [(ngModel)]="password"
                  name="password"
                  placeholder="••••••••"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 transition"
                  [disabled]="loading()"
                />
              </div>

              <!-- Submit Button -->
              <button
                type="submit"
                [disabled]="loading()"
                class="w-full px-6 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed mt-6"
              >
                <span *ngIf="!loading()">Acceder</span>
                <span *ngIf="loading()" class="flex items-center justify-center gap-2">
                  <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  Verificando...
                </span>
              </button>
            </form>

            <!-- Back Link -->
            <div class="mt-6 text-center">
              <a routerLink="/" class="text-blue-600 hover:text-blue-700 text-sm font-medium">
                ← Volver al sitio
              </a>
            </div>
          </div>

          <!-- Info Box -->
          <div class="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-blue-700 text-sm">
              <span class="font-semibold">Nota:</span> Este portal está restringido. Solo admins pueden acceder.
            </p>
          </div>
        </div>
      </main>
    </div>
  `,
  styles: []
})
export class AdminLoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal('');
  isUserNotAdmin = signal(false);

  handleLogin(): void {
    if (!this.email || !this.password) {
      this.error.set('Por favor completa todos los campos');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.isUserNotAdmin.set(false);

    this.authService 
      .login({ email: this.email, password: this.password })
      .subscribe({
        next: (response) => {
          // Verificar si el rol es ADMIN
          if (response.role === 'ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            // Usuario logeado pero no es admin
            this.loading.set(false);
            this.isUserNotAdmin.set(true);
            this.authService.logout();
            this.error.set('Acceso denegado. Este portal es solo para administradores.');
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Error al iniciar sesión. Verifica tus credenciales.');
        }
      });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/admin/login']);
  }
}
