import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

export interface PaymentPage {
  content: PaymentResponseDTO[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
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

  /**
   * Obtiene todos los pagos (admin)
   */
  getPayments(page: number = 0, size: number = 20): Observable<PaymentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PaymentPage>(this.apiUrl, { params });
  }

  /**
   * Obtiene un pago por ID
   */
  getPaymentById(id: number): Observable<PaymentResponseDTO> {
    return this.http.get<PaymentResponseDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Cancela un pago y su PaymentIntent en Stripe
   */
  cancelPayment(paymentId: number): Observable<PaymentResponseDTO> {
    return this.http.post<PaymentResponseDTO>(`${this.apiUrl}/${paymentId}/cancel`, {});
  }

  /**
   * Obtiene pagos por estado
   */
  getPaymentsByStatus(status: string, page: number = 0, size: number = 20): Observable<PaymentPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PaymentPage>(`${this.apiUrl}/status/${status}`, { params });
  }
}
