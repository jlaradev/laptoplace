import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentResponseDTO } from '../payment/payment.service';

export interface CreateOrderDTO {
  direccionEnvio: string;
}

export interface OrderItem {
  id: number;
  product: {
    id: number;
    nombre: string;
    precio: number;
    imagenUrl?: string;
  };
  cantidad: number;
  precioUnitario: number;
}

export interface OrderResponseDTO {
  id: number;
  userId: string;
  total: number;
  estado: string;
  direccionEnvio: string;
  expiresAt?: string;
  items?: OrderItem[];
  payment?: PaymentResponseDTO;
  createdAt?: string;
}

export interface OrderPage {
  content: OrderResponseDTO[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

export interface PurchasedResponseDTO {
  purchased: boolean;
  hasReview: boolean;
  reviewId: number | null;
}

export interface ReviewableProductDTO {
  id: number;
  nombre: string;
  precio: number;
  hasReview: boolean;
  reviewId?: number | null;
  imagenPrincipal?: {
    url: string;
  };
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = (window as any).APP_CONFIG?.apiBaseUrl ? `${(window as any).APP_CONFIG.apiBaseUrl.replace(/\/$/, '')}/api/orders` : '/api/orders';

  constructor(private http: HttpClient) {}

  createOrderFromCart(userId: string, dto: CreateOrderDTO): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.apiUrl}/user/${userId}`, dto);
  }

  /**
   * Obtiene todas las órdenes del usuario autenticado
   */
  getUserOrders(userId: string, page: number = 0, size: number = 10): Observable<OrderPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<OrderPage>(`${this.apiUrl}/user/${userId}`, { params });
  }

  /**
   * Obtiene una orden por ID
   */
  getOrderById(orderId: number): Observable<OrderResponseDTO> {
    return this.http.get<OrderResponseDTO>(`${this.apiUrl}/${orderId}`);
  }

  /**
   * Obtiene todas las órdenes (admin)
   */
  getOrders(page: number = 0, size: number = 20): Observable<OrderPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<OrderPage>(this.apiUrl, { params });
  }

  /**
   * Obtiene órdenes por estado específico
   */
  getOrdersByStatus(status: string, page: number = 0, size: number = 10): Observable<OrderPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<OrderPage>(`${this.apiUrl}/status/${status}`, { params });
  }

  /**
   * Obtiene órdenes activas del usuario (PROCESANDO, ENVIADO, ENTREGADO)
   */
  getUserActiveOrders(userId: string, page: number = 0, size: number = 20): Observable<OrderPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<OrderPage>(`${this.apiUrl}/user/${userId}/active`, { params });
  }

  /**
   * Cancela una orden pendiente de pago
   */
  cancelOrder(orderId: number): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.apiUrl}/${orderId}/cancel`, {});
  }

  /**
   * Verifica si el usuario ha comprado un producto específico
   */
  isProductPurchased(userId: string, productId: number): Observable<PurchasedResponseDTO> {
    return this.http.get<PurchasedResponseDTO>(`${this.apiUrl}/user/${userId}/product/${productId}/purchased`);
  }

  /**
   * Obtiene los productos que un usuario puede reseñar
   */
  getReviewableProducts(userId: string, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/user/${userId}/reviewable-products`, { params });
  }
}
