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
   * Obtiene órdenes por estado específico
   */
  getOrdersByStatus(status: string, page: number = 0, size: number = 10): Observable<OrderPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<OrderPage>(`${this.apiUrl}/status/${status}`, { params });
  }
}
