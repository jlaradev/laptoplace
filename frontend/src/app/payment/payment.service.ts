import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreatePaymentDTO {
  orderId: number;
  amount: number;
}

export interface PaymentResponseDTO {
  id: number;
  orderId: number;
  stripePaymentId: string;
  clientSecret: string;
  monto: number;
  estado: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  // Use API base from public config if available, otherwise fallback to relative path
  private apiUrl = (window as any).APP_CONFIG?.apiBaseUrl ? `${(window as any).APP_CONFIG.apiBaseUrl.replace(/\/$/, '')}/api/payments` : '/api/payments';

  constructor(private http: HttpClient) {}

  createPayment(dto: CreatePaymentDTO): Observable<PaymentResponseDTO> {
    console.log('[PaymentService] POST', `${this.apiUrl}/create`, dto);
    return this.http.post<PaymentResponseDTO>(`${this.apiUrl}/create`, dto);
  }
}
