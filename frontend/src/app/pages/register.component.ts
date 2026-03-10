import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { FooterComponent } from '../components/footer.component';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FooterComponent],
  template: `
    <div class="flex flex-col min-h-screen bg-white">
      <!-- Header -->
      <header class="border-b border-slate-200 sticky top-0 bg-white z-40">
        <div class="max-w-[1440px] mx-auto px-4 md:px-6 py-5 flex items-center justify-between">
          <a routerLink="/" class="flex items-center text-xl font-bold text-slate-900">Lapto<span class="text-blue-600">Place</span></a>
        </div>
      </header>

      <!-- Main Content -->
      <main class="flex-1 flex items-center justify-center px-4">
        <div class="w-full max-w-md">
          <div class="bg-white border border-slate-200 rounded-2xl p-8 shadow-sm">
            <div class="mb-8">
              <h1 class="text-3xl font-bold text-slate-900 mb-2">Crear cuenta</h1>
              <p class="text-slate-600">Regístrate para comprar y comparar laptops</p>
            </div>

            <div *ngIf="error" class="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-red-700 text-sm font-semibold">{{ error }}</p>
            </div>
            <div *ngIf="success" class="mb-6 p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-green-700 text-sm font-semibold">{{ success }}</p>
            </div>

            <form (ngSubmit)="handleRegister()" class="space-y-4">
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Correo *</label>
                <input type="email" [(ngModel)]="email" name="email" placeholder="tu@correo.com" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Nombre *</label>
                <input type="text" [(ngModel)]="nombre" name="nombre" placeholder="Tu nombre" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Apellido *</label>
                <input type="text" [(ngModel)]="apellido" name="apellido" placeholder="Tu apellido" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Contraseña *</label>
                <input type="password" [(ngModel)]="password" name="password" placeholder="••••••••" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Teléfono</label>
                <input type="text" [(ngModel)]="telefono" name="telefono" placeholder="Opcional" class="w-full px-4 py-2 border border-slate-200 rounded-lg" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Dirección</label>
                <input type="text" [(ngModel)]="direccion" name="direccion" placeholder="Opcional" class="w-full px-4 py-2 border border-slate-200 rounded-lg" />
              </div>
              <button type="submit" [disabled]="loading" class="w-full px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed">
                <span *ngIf="!loading">Crear cuenta</span>
                <span *ngIf="loading" class="flex items-center justify-center gap-2">
                  <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  Creando...
                </span>
              </button>
            </form>

            <p class="text-center text-slate-600 text-sm mt-6">
              ¿Ya tienes cuenta? <a routerLink="/login" class="text-blue-600 hover:underline font-semibold">Inicia sesión aquí</a>
            </p>
          </div>
        </div>
      </main>

      <app-footer></app-footer>
    </div>
  `
})
export class RegisterComponent {
  email = '';
  nombre = '';
  apellido = '';
  password = '';
  telefono = '';
  direccion = '';
  error: string | null = null;
  success: string | null = null;
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  handleRegister() {
    this.error = null;
    this.success = null;
    this.loading = true;
    if (!this.email || !this.nombre || !this.apellido || !this.password) {
      this.error = 'Completa los campos obligatorios.';
      this.loading = false;
      return;
    }
    this.authService.registerUser({
      email: this.email,
      nombre: this.nombre,
      apellido: this.apellido,
      password: this.password,
      telefono: this.telefono,
      direccion: this.direccion
    }).subscribe({
      next: () => {
        this.success = 'Usuario creado correctamente. Ahora puedes iniciar sesión.';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al crear usuario.';
        this.loading = false;
      }
    });
  }
}
