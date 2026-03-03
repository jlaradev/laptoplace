import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Brand, BrandPage } from '../models/brand.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BrandService {
  private apiUrl = `${environment.apiBaseUrl}/api/brands`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene todas las marcas
   */
  getAllBrands(): Observable<Brand[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(response => {
        // Si la respuesta tiene estructura de paginación, extrae el content
        return Array.isArray(response) ? response : response.content || [];
      })
    );
  }

  /**
   * Obtiene una marca por ID
   */
  getBrandById(id: number): Observable<Brand> {
    return this.http.get<Brand>(`${this.apiUrl}/${id}`);
  }
}
