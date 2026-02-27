import { Component, inject, signal, OnInit, OnDestroy, ElementRef } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';
import { UserService, User } from '../services/user.service';

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
          <ng-container *ngIf="isLoggedIn && !isOnCartPage">
            <div class="relative mr-2">
              <button (click)="toggleCartDropdown()" class="relative px-3 py-2 rounded-full bg-blue-50 hover:bg-blue-100 transition flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.35 2.7A1 1 0 007.5 17h9a1 1 0 00.85-1.53L17 13M7 13V6a1 1 0 011-1h5a1 1 0 011 1v7" />
                </svg>
                <span *ngIf="(cartRawArrayLength() > 0)" class="absolute -top-1 -right-1 bg-red-600 text-white text-xs rounded-full px-1.5">{{ cartRawArrayLength() }}</span>
              </button>
              <div *ngIf="showCartDropdown()" class="absolute right-0 mt-2 w-80 bg-white border border-slate-200 rounded-lg shadow-lg z-50">
                <div *ngIf="getItems().length === 0" class="p-4">
                  <span class="text-slate-500">Tu carrito está vacío</span>
                </div>
                <div *ngIf="getItems().length > 0">
                  <div *ngFor="let item of getItems()" class="flex items-center gap-3 px-4 py-2 border-b last:border-b-0">
                    <img *ngIf="item.product?.imagenUrl" [src]="item.product.imagenUrl" alt="img" class="w-12 h-12 object-contain rounded" />
                    <div class="flex-1">
                      <div class="font-semibold text-slate-900 text-sm">{{ item.product?.nombre }}</div>
                      <div class="text-xs text-slate-600">Cantidad: {{ item.cantidad }}</div>
                    </div>
                    <div class="text-sm font-bold text-blue-700">$ {{ item.product?.precio | number:'1.2-2' }}</div>
                  </div>
                  <div class="p-4 text-right">
                    <div class="font-semibold mb-2">Total: $ {{ getTotal() }}</div>
                    <a routerLink="/cart" class="text-blue-700 font-semibold hover:underline">Ver carrito</a>
                  </div>
                </div>
              </div>
            </div>
          </ng-container>
          <span *ngIf="isLoggedIn && userName" class="text-blue-700 font-bold text-base mr-2">BIENVENIDO, {{ userName }}</span>
          <button *ngIf="!isLoggedIn" (click)="goToLogin()" class="px-5 py-2 text-sm font-semibold text-slate-700 border border-slate-200 rounded-full hover:bg-slate-50 transition cursor-pointer">
            Iniciar sesion
          </button>
          <div *ngIf="isLoggedIn" class="relative">
            <button (click)="toggleUserDropdown()" class="px-5 py-2 text-sm font-semibold text-white bg-blue-600 rounded-full hover:bg-blue-700 transition flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.121 17.804A13.937 13.937 0 0112 15c2.5 0 4.847.657 6.879 1.804M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
              Usuario
            </button>
            <div *ngIf="showUserDropdown" class="absolute right-0 mt-2 w-48 bg-white border border-slate-200 rounded-lg shadow-lg z-50">
              <button (click)="goToProfile()" class="w-full text-left px-4 py-2 hover:bg-blue-50 text-blue-700 font-semibold">Mi perfil</button>
              <button (click)="logout()" class="w-full text-left px-4 py-2 hover:bg-red-50 text-red-600 font-semibold">Cerrar sesión</button>
            </div>
          </div>
        </div>
      </div>
    </header>
  `,
  styles: []
})
export class HeaderComponent implements OnInit, OnDestroy {
    toggleUserDropdown() {
      this.showUserDropdown = !this.showUserDropdown;
    }

    goToProfile() {
      this.router.navigate(['/profile']);
      this.showUserDropdown = false;
    }
  showUserDropdown = false;
  // ...existing code...
  private authService = inject(AuthService);
  private cartService = inject(CartService);
  private router = inject(Router);
  isLoggedIn = this.authService.isLoggedInSync();
  userName: string | null = null;
  private userService = inject(UserService);
  cartRaw = signal<any>(null);
  showCartDropdown = signal(false);
  private clickListener: any;
  private el = inject(ElementRef);
  private routerSub?: Subscription;
  isOnCartPage = false;

  constructor() {
      // ...existing code...
    // keep constructor lightweight; initialization in ngOnInit
  }

  ngOnInit(): void {
      // ...existing code...
    this.isLoggedIn = this.authService.isLoggedInSync();
    if (this.isLoggedIn) {
      const userId = localStorage.getItem('userId');
      if (userId) {
        this.userService.getUserById(userId).subscribe({
          next: (u: User) => {
            this.userName = u.nombre;
            this.loadCart();
          },
          error: () => {
            this.userName = null;
            this.loadCart();
          }
        });
      } else {
        this.userName = null;
        this.loadCart();
      }
    }
    // Suscribirse a cambios en el carrito para recargar o aplicar cambios parciales
    this.cartService.cartChanged$.subscribe((ev: any) => {
      if (!this.authService.isLoggedInSync()) return;
      if (!ev || ev.type === 'refresh') {
        this.loadCart();
        return;
      }
      if (ev.type === 'item-updated') {
        const payload = ev.payload;
        // actualizar sólo el item en el signal para evitar recarga completa y reordenamientos
        setTimeout(() => {
          this.cartRaw.update((curr: any) => {
            if (!curr || !curr.items) return curr;
            const items = curr.items.map((it: any) => {
              if (it.id === payload.id) {
                const updated = { ...it, cantidad: payload.cantidad };
                return updated;
              }
              return it;
            });
            const total = items.reduce((s: number, it: any) => s + (it.product?.precio || 0) * (it.cantidad || 0), 0);
            return { ...curr, items, total };
          });
        }, 0);
      }
    });

    // Close dropdown when clicking outside
    this.clickListener = (event: Event) => {
      if (this.showCartDropdown() && !this.el.nativeElement.contains(event.target)) {
        this.showCartDropdown.set(false);
      }
    };
    document.addEventListener('click', this.clickListener, true);

    // track route so we can hide/disable the cart icon on the cart page
    this.isOnCartPage = this.router.url === '/cart';
    this.routerSub = this.router.events.subscribe((ev: any) => {
      if (ev instanceof NavigationEnd) {
        this.isOnCartPage = ev.urlAfterRedirects === '/cart' || ev.url === '/cart';
      }
    });
  }

  ngOnDestroy(): void {
      // ...existing code...
    if (this.clickListener) document.removeEventListener('click', this.clickListener, true);
    this.routerSub?.unsubscribe();
  }

  loadCart() {
    this.cartService.getCart().subscribe({
      next: data => {
        console.log('[Header] loadCart response:', data);
        // overlay pending optimistic updates so dropdown doesn't revert
        const pending = this.cartService.getPendingUpdates?.() || {};
        if (data && data.items && Object.keys(pending).length > 0) {
          data = { ...data, items: data.items.map((it: any) => {
            if (pending[it.id] != null) {
              return { ...it, cantidad: pending[it.id] };
            }
            return it;
          }) };
          // recalc total using possibly-overlaid quantities
          const total = (data.items || []).reduce((s: number, it: any) => s + (it.product?.precio || 0) * (it.cantidad || 0), 0);
          data.total = total;
        }
        // actualizar el signal de forma asíncrona para evitar ExpressionChangedAfterItHasBeenCheckedError
        setTimeout(() => this.cartRaw.set(data), 0);
      },
      error: (err) => {
        console.error('[Header] loadCart error:', err);
        setTimeout(() => this.cartRaw.set(null), 0);
      }
    });
  }

  cartRawArrayLength(): number {
    const v = this.cartRaw();
    if (Array.isArray(v)) return v.length;
    if (v && v.items && Array.isArray(v.items)) return v.items.length;
    return 0;
  }

  getItems(): any[] {
    const v = this.cartRaw();
    if (!v) return [];
    if (Array.isArray(v)) return v;
    if (v.items && Array.isArray(v.items)) return v.items;
    return [];
  }

  getTotal(): string {
    const v = this.cartRaw();
    if (!v) return '0.00';
    const total = v.total ?? v.totalAmount ?? 0;
    const n = typeof total === 'string' ? parseFloat(total) : Number(total);
    return isNaN(n) ? '0.00' : n.toFixed(2);
  }

  toggleCartDropdown() {
    this.showCartDropdown.update(v => !v);
    if (this.showCartDropdown()) {
      this.loadCart();
    }
  }

  // ...existing code...

  goToLogin() {
    this.router.navigate(['/login'], { queryParams: { redirect: this.router.url } });
  }

  logout() {
    this.authService.logout();
    window.location.reload();
  }
}
