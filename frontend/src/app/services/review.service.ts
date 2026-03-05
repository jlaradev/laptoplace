import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateReviewDTO {
  productId: number;
  rating: number;
  comentario: string;
}

export interface ReviewResponseDTO {
  id: number;
  productId: number;
  userId: string;
  userNombre: string;
  rating: number;
  comentario: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private apiUrl = 'https://laptophub-cigv.onrender.com/api/reviews';

  constructor(private http: HttpClient) {}

  createReview(dto: CreateReviewDTO, userId: string): Observable<ReviewResponseDTO> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<ReviewResponseDTO>(`${this.apiUrl}`, dto, { params });
  }

  getReviewsByProduct(productId: number, page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/product/${productId}?page=${page}&size=${size}`);
  }

  getUserReviewForProduct(productId: number, userId: string): Observable<ReviewResponseDTO> {
    return this.http.get<ReviewResponseDTO>(`${this.apiUrl}/product/${productId}/user/${userId}`);
  }

  updateReview(reviewId: number, dto: Partial<CreateReviewDTO>): Observable<ReviewResponseDTO> {
    return this.http.put<ReviewResponseDTO>(`${this.apiUrl}/${reviewId}`, dto);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${reviewId}`);
  }

  getAverageRating(productId: number): Observable<{ average: number }> {
    return this.http.get<{ average: number }>(`${this.apiUrl}/product/${productId}/average`);
  }
}
