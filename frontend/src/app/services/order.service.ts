import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentResponseDTO } from '../payment/payment.service';

export interface CreateOrderDTO {
  direccionEnvio: string;
}

export interface OrderResponseDTO {
  id: number;
  userId: string;
  total: number;
  estado: string;
  direccionEnvio: string;
  expiresAt?: string;
  items?: any[];
  payment?: PaymentResponseDTO;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = (window as any).APP_CONFIG?.apiBaseUrl ? `${(window as any).APP_CONFIG.apiBaseUrl.replace(/\/$/, '')}/api/orders` : '/api/orders';

  constructor(private http: HttpClient) {}

  createOrderFromCart(userId: string, dto: CreateOrderDTO): Observable<OrderResponseDTO> {
    return this.http.post<OrderResponseDTO>(`${this.apiUrl}/user/${userId}`, dto);
  }
}
