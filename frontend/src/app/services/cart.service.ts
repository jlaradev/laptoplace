import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface CartItem {
  id: number;
  productId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  imagenUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private apiUrl = 'https://laptophub-cigv.onrender.com/api/cart';

  // Emite cuando el carrito cambia. Payload: { type: 'refresh'|'item-updated', payload?: any }
  private cartChanged = new Subject<any>();
  cartChanged$ = this.cartChanged.asObservable();

  // Cache simple del último carrito obtenido
  private lastCart: any = null;
  // Pending optimistic updates: itemId -> cantidad
  private pendingUpdates = new Map<number, number>();

  // Obtener el userId desde localStorage
  private getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  constructor(private http: HttpClient) {}


  getCart(): Observable<any> {
    const userId = this.getUserId();
    if (!userId) {
      // No user logged in: return empty cart observable instead of throwing
      const empty = { items: [], total: 0 };
      this.lastCart = empty;
      return of(empty);
    }
    return this.http.get<any>(`${this.apiUrl}/user/${userId}`).pipe(
      tap(data => this.lastCart = data)
    );
  }


  addToCart(productId: number, cantidad: number = 1): Observable<any> {
    const userId = this.getUserId();
    if (!userId) throw new Error('No userId found in localStorage');
    return this.http.post(`${this.apiUrl}/user/${userId}/items`, { productId, cantidad }).pipe(
      tap(() => this.cartChanged.next({ type: 'refresh' }))
    );
  }


  removeFromCart(itemId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/items/${itemId}`).pipe(
      tap(() => this.cartChanged.next({ type: 'refresh' }))
    );
  }


  clearCart(): Observable<any> {
    const userId = this.getUserId();
    const token = localStorage.getItem('token');
    if (!userId) throw new Error('No userId found in localStorage');
    if (!token) throw new Error('No token found in localStorage');
    return this.http.delete(`${this.apiUrl}/user/${userId}/clear`, {
      headers: { Authorization: `Bearer ${token}` }
    }).pipe(
      tap(() => this.cartChanged.next({ type: 'refresh' }))
    );
  }

  // Permite notificar manualmente cambios (optimistic updates)
  notifyCartChanged() {
    this.cartChanged.next({ type: 'refresh' });
  }

  getCachedCart() {
    return this.lastCart;
  }

  updateItemQuantity(itemId: number, cantidad: number): Observable<any> {
    if (cantidad <= 0) {
      return this.removeFromCart(itemId);
    }
    return this.http.put(`${this.apiUrl}/items/${itemId}`, { cantidad }).pipe(
      tap(() => {
        // server confirmed -> clear pending and notify
        this.pendingUpdates.delete(itemId);
        this.cartChanged.next({ type: 'item-updated', payload: { id: itemId, cantidad } });
      }),
      catchError((error) => {
        // clear pending and request full refresh so UI syncs with server
        this.pendingUpdates.delete(itemId);
        this.cartChanged.next({ type: 'refresh' });
        throw error;
      })
    );
  }

  // Emitir una actualización optimista para que otros componentes se sincronicen inmediatamente
  optimisticUpdateItem(itemId: number, cantidad: number) {
    this.pendingUpdates.set(itemId, cantidad);
    this.cartChanged.next({ type: 'item-updated', payload: { id: itemId, cantidad } });
  }

  getPendingUpdates() {
    // return plain object for easier merging
    const obj: Record<number, number> = {};
    this.pendingUpdates.forEach((v, k) => obj[k] = v);
    return obj;
  }
}
