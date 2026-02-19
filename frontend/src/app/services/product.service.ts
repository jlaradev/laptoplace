import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductPage } from '../models/product.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = `${environment.apiBaseUrl}/api/products`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene una lista paginada de productos
   * @param page Número de página (comienza en 0)
   * @param size Cantidad de productos por página
   */
  getProducts(page: number = 0, size: number = 12): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  /**
   * Obtiene un producto por ID
   */
  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  /**
   * Busca productos por nombre
   */
  searchByName(nombre: string, page: number = 0, size: number = 12): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}/search?nombre=${nombre}&page=${page}&size=${size}`);
  }

  /**
   * Busca productos por marca
   */
  findByBrand(marca: string, page: number = 0, size: number = 12): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}/brand?marca=${marca}&page=${page}&size=${size}`);
  }
}
