import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductPage, ProductImage } from '../models/product.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = `${environment.apiBaseUrl}/api/products`;

  constructor(private http: HttpClient) {}

  /**
   * Búsqueda unificada con todos los parámetros
   */
  search(options: {
    nombre?: string;
    brandId?: number;
    sortBy?: string;
    sort?: string;
    page?: number;
    size?: number;
  }): Observable<ProductPage> {
    const params = new URLSearchParams();
    if (options.nombre && options.nombre.trim()) params.append('nombre', options.nombre);
    if (options.brandId !== null && options.brandId !== undefined) params.append('brandId', options.brandId.toString());
    if (options.sortBy) params.append('sortBy', options.sortBy);
    if (options.sort) params.append('sort', options.sort);
    params.append('page', (options.page ?? 0).toString());
    params.append('size', (options.size ?? 20).toString());

    return this.http.get<ProductPage>(`${this.apiUrl}?${params.toString()}`);
  }

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
  searchByName(nombre: string, page: number = 0, size: number = 20): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}?nombre=${nombre}&page=${page}&size=${size}`);
  }

  /**
   * Busca productos por marca
   */
  findByBrand(marca: string, page: number = 0, size: number = 12): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}/brand?marca=${marca}&page=${page}&size=${size}`);
  }

  /**
   * Obtiene los 10 mejores productos valorados
   */
  getTopRatedProducts(): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.apiUrl}/top-rated`);
  }

  /**
   * ADMIN: Crear un nuevo producto
   */
  createProduct(product: Omit<Product, 'id' | 'promedioRating'>): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product);
  }

  /**
   * ADMIN: Actualizar un producto existente
   */
  updateProduct(id: number, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
  }

  /**
   * ADMIN: Eliminar un producto
   */
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

    /**
     * Obtiene productos inactivos (eliminados lógicamente)
     */
    getInactiveProducts(page: number = 0, size: number = 20): Observable<ProductPage> {
      return this.http.get<ProductPage>(`${this.apiUrl}/inactive?page=${page}&size=${size}`);
    }

    /**
     * Reactiva un producto eliminado
     */
    reactivateProduct(id: number): Observable<Product> {
      return this.http.put<Product>(`${this.apiUrl}/${id}/reactivate`, {});
    }

  /**
   * ADMIN: Subir imagen para un producto
   */
  uploadImage(productId: number, file: File, orden: number = 1, descripcion?: string): Observable<ProductImage> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('orden', orden.toString());
    if (descripcion) formData.append('descripcion', descripcion);
    
    return this.http.post<ProductImage>(`${this.apiUrl}/${productId}/images`, formData);
  }

  /**
   * ADMIN: Obtener todas las imágenes de un producto
   */
  getProductImages(productId: number): Observable<ProductImage[]> {
    return this.http.get<ProductImage[]>(`${this.apiUrl}/${productId}/images`);
  }

  /**
   * ADMIN: Eliminar imagen de un producto
   */
  deleteImage(imageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/images/${imageId}`);
  }

  /**
   * ADMIN: Actualizar imagen (orden, descripción, etc.)
   * El backend espera query params: ?url=...&orden=...&descripcion=...
   */
  updateImage(imageId: number, updates: Partial<ProductImage>): Observable<ProductImage> {
    let params = new HttpParams();
    
    if (updates.url !== undefined && updates.url !== null) {
      params = params.set('url', updates.url);
    }
    if (updates.orden !== undefined && updates.orden !== null) {
      params = params.set('orden', updates.orden.toString());
    }
    if (updates.descripcion !== undefined && updates.descripcion !== null) {
      params = params.set('descripcion', updates.descripcion);
    }
    
    return this.http.put<ProductImage>(`${this.apiUrl}/images/${imageId}`, null, { params });
  }
}
