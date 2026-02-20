import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProductDetail {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  stock: number;
  marca: string;
  procesador: string;
  ram: number | string;
  almacenamiento: number | string;
  pantalla: string;
  gpu: string;
  peso: number | string;
  imagenes: { id: number; url: string; orden: number; descripcion: string }[];
  resenas: { id: number; productId: number; userId: string; userNombre: string; rating: number; comentario: string; createdAt: string }[];
  promedioRating: number;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ProductDetailService {
  private apiUrl = 'https://laptophub-cigv.onrender.com/api/products';

  constructor(private http: HttpClient) {}

  getProductDetail(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.apiUrl}/${id}`);
  }
}
