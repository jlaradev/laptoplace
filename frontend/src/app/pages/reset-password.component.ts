import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col min-h-screen bg-white items-center justify-center">
      <div class="w-full max-w-md bg-white border border-slate-200 rounded-2xl p-8 shadow-sm mt-16">
        <h1 class="text-2xl font-bold mb-4">Restablecer contraseña</h1>
        <form (ngSubmit)="handleReset()" class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-2">Nueva contraseña</label>
            <input type="password" [(ngModel)]="password" name="password" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-2">Confirmar contraseña</label>
            <input type="password" [(ngModel)]="confirmPassword" name="confirmPassword" class="w-full px-4 py-2 border border-slate-200 rounded-lg" required />
          </div>
          <button type="submit" [disabled]="loading" class="w-full px-6 py-2 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed">
            <span *ngIf="!loading">Restablecer</span>
            <span *ngIf="loading" class="flex items-center justify-center gap-2">
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              Restableciendo...
            </span>
          </button>
        </form>
        <div *ngIf="message" class="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm font-semibold">{{ message }}</div>
        <div *ngIf="error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm font-semibold">{{ error }}</div>
      </div>
    </div>
  `
})
export class ResetPasswordComponent {
  password = '';
  confirmPassword = '';
  loading = false;
  message: string | null = null;
  error: string | null = null;

  constructor(private authService: AuthService, private router: Router) {}

  handleReset() {
    this.message = null;
    this.error = null;
    this.loading = true;
    const url = new URL(window.location.href);
    const token = url.searchParams.get('token');
    if (!token) {
      this.error = 'Token inválido.';
      this.loading = false;
      return;
    }
    if (!this.password || !this.confirmPassword) {
      this.error = 'Completa ambos campos.';
      this.loading = false;
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
      this.loading = false;
      return;
    }
    this.authService.resetPassword(token, this.password).subscribe({
      next: () => {
        this.message = 'Contraseña restablecida correctamente.';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al restablecer la contraseña.';
        this.loading = false;
      }
    });
  }
}
