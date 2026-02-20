import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="border-b border-slate-200 sticky top-0 bg-white z-40">
      <div class="max-w-[1440px] mx-auto px-4 md:px-6 py-5 flex items-center justify-between">
        <a href="/" class="flex items-center text-xl font-bold text-slate-900">Lapto<span class="text-blue-600">Place</span></a>
        <nav class="hidden md:flex gap-8 text-sm font-semibold text-slate-700">
          <a routerLink="/catalog" class="hover:text-blue-600 transition">Catalogo</a>
          <a routerLink="/compare" class="hover:text-blue-600 transition">Comparar equipos</a>
        </nav>
        <div class="flex gap-3 items-center">
          <span *ngIf="isLoggedIn && userEmail" class="text-blue-700 font-bold text-base mr-2">BIENVENIDO, {{ userEmail }}</span>
          <a *ngIf="!isLoggedIn" routerLink="/login" class="px-5 py-2 text-sm font-semibold text-slate-700 border border-slate-200 rounded-full hover:bg-slate-50 transition cursor-pointer">
            Iniciar sesion
          </a>
          <button *ngIf="isLoggedIn" (click)="logout()" class="px-5 py-2 text-sm font-semibold text-white bg-red-600 rounded-full hover:bg-red-700 transition">
            Cerrar sesion
          </button>
        </div>
      </div>
    </header>
  `,
  styles: []
})
export class HeaderComponent {
  private authService = inject(AuthService);
  isLoggedIn = this.authService.isLoggedInSync();
  userEmail: string | null = null;

  constructor() {
    if (this.isLoggedIn) {
      this.userEmail = this.authService.getUserEmail();
    }
  }

  logout() {
    this.authService.logout();
    window.location.reload();
  }
}
