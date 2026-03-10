import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { FooterComponent } from '../components/footer.component';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
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
              <h1 class="text-3xl font-bold text-slate-900 mb-2">Iniciar sesión</h1>
              <p class="text-slate-600">Accede a tu cuenta de LaptoPlace</p>
            </div>

            <!-- Error Message -->
            <div *ngIf="error()" class="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-red-700 text-sm font-semibold">{{ error() }}</p>
            </div>

            <!-- Login Form -->
            <form (ngSubmit)="handleLogin()" class="space-y-4">
              <!-- Email Input -->
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Correo</label>
                <input
                  type="email"
                  [(ngModel)]="email"
                  name="email"
                  placeholder="tu@correo.com"
                  class="w-full px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 transition"
                  [disabled]="loading()"
                />
              </div>

              <!-- Password Input -->
              <div>
                <label class="block text-sm font-semibold text-slate-900 mb-2">Contraseña</label>
                <input
                  type="password"
                  [(ngModel)]="password"
                  name="password"
                  placeholder="••••••••"
                  class="w-full px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 transition"
                  [disabled]="loading()"
                />
              </div>

              <!-- Submit Button -->
              <button
                type="submit"
                [disabled]="loading()"
                class="w-full px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed mt-6"
              >
                <span *ngIf="!loading()">Iniciar sesión</span>
                <span *ngIf="loading()" class="flex items-center justify-center gap-2">
                  <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  Iniciando...
                </span>
              </button>
            </form>
            <!-- Forgot Password Button -->
            <div class="mt-4 text-center">
              <button
                type="button"
                class="text-blue-600 hover:underline text-sm font-semibold"
                (click)="showForgot = true"
                *ngIf="!showForgot"
              >¿Olvidaste tu contraseña?</button>
            </div>

            <!-- Forgot Password Form -->
            <div *ngIf="showForgot" class="mt-6">
              <h2 class="text-xl font-bold mb-2">Recuperar contraseña</h2>
              <form (ngSubmit)="handleForgotPassword()" class="space-y-4">
                <div>
                  <label class="block text-sm font-semibold text-slate-900 mb-2">Correo electrónico</label>
                  <input
                    type="email"
                    [(ngModel)]="forgotEmail"
                    name="forgotEmail"
                    placeholder="tu@correo.com"
                    class="w-full px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 transition"
                    [disabled]="forgotLoading()"
                  />
                </div>
                <button
                  type="submit"
                  [disabled]="forgotLoading()"
                  class="w-full px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span *ngIf="!forgotLoading()">Enviar enlace</span>
                  <span *ngIf="forgotLoading()" class="flex items-center justify-center gap-2">
                    <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    Enviando...
                  </span>
                </button>
                <button
                  type="button"
                  class="w-full px-6 py-2 bg-slate-200 text-slate-700 rounded-full font-semibold hover:bg-slate-300 transition mt-2"
                  (click)="showForgot = false"
                >Cancelar</button>
              </form>
              <div *ngIf="forgotMessage" class="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm font-semibold">{{ forgotMessage }}</div>
              <div *ngIf="forgotError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm font-semibold">{{ forgotError }}</div>
            </div>

            <!-- Info -->
            <p class="text-center text-slate-600 text-sm mt-6">
              ¿Aún no tienes cuenta? <a routerLink="/register" class="text-blue-600 hover:underline font-semibold">Regístrate aquí</a>
            </p>
          </div>
        </div>
      </main>

      <app-footer></app-footer>
    </div>
  `,
  styles: []
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  showForgot = false;
  forgotEmail = '';
  forgotMessage: string | null = null;
  forgotError: string | null = null;
  forgotLoading = signal(false);

  constructor() {
    // Si ya está autenticado, redirigir automáticamente
    if (this.authService.isLoggedInSync()) {
      const redirect = this.route.snapshot.queryParamMap.get('redirect');
      if (redirect) {
        this.router.navigateByUrl(redirect);
      } else {
        this.router.navigate(['/']);
      }
    }
  }

  handleLogin() {
    this.error.set(null);

    if (!this.email || !this.password) {
      this.error.set('Por favor completa todos los campos');
      return;
    }

    this.loading.set(true);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        if (redirect) {
          this.router.navigateByUrl(redirect);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Error al iniciar sesión. Por favor intenta de nuevo.');
      }
    });
  }

  handleForgotPassword() {
    this.forgotMessage = null;
    this.forgotError = null;
    if (!this.forgotEmail) {
      this.forgotError = 'Por favor ingresa tu correo electrónico';
      return;
    }
    this.forgotLoading.set(true);
    this.authService.forgotPassword(this.forgotEmail).subscribe({
      next: () => {
        this.forgotLoading.set(false);
        this.forgotMessage = 'Enlace para restablecer contraseña enviado.';
      },
      error: (err) => {
        this.forgotLoading.set(false);
        this.forgotError = err.error?.message || 'Error al enviar el enlace. Intenta de nuevo.';
      }
    });
  }
}

