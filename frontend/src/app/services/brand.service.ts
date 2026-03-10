import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Brand, BrandPage } from '../models/brand.model';
import { environment } from '../../environments/environment';

export interface BrandCreateDTO {
  nombre: string;
  descripcion?: string;
  imageUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BrandService {
  private apiUrl = `${environment.apiBaseUrl}/api/brands`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene todas las marcas con paginación
   */
  getBrands(page: number = 0, size: number = 20): Observable<BrandPage> {
    let params = new HttpParams();
    params = params.set('page', page.toString());
    params = params.set('size', size.toString());
    
    return this.http.get<BrandPage>(this.apiUrl, { params });
  }

  /**
   * ADMIN: Subir imagen para una marca (backend sube a Cloudinary)
   */
  uploadBrandImage(id: number, file: File): Observable<Brand> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Brand>(`${this.apiUrl}/${id}/image`, formData);
  }

  /**
   * Obtiene todas las marcas sin paginación
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

  /**
   * ADMIN: Crear nueva marca
   */
  createBrand(dto: BrandCreateDTO): Observable<Brand> {
    return this.http.post<Brand>(this.apiUrl, dto);
  }

  /**
   * ADMIN: Actualizar marca
   */
  updateBrand(id: number, dto: BrandCreateDTO): Observable<Brand> {
    return this.http.put<Brand>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * ADMIN: Eliminar marca
   */
  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

    /**
     * Obtiene marcas inactivas (eliminadas lógicamente)
     */
    getInactiveBrands(page: number = 0, size: number = 20): Observable<BrandPage> {
      let params = new HttpParams();
      params = params.set('page', page.toString());
      params = params.set('size', size.toString());
      return this.http.get<BrandPage>(`${this.apiUrl}/inactive`, { params });
    }

    /**
     * Reactiva una marca eliminada
     */
    reactivateBrand(id: number): Observable<Brand> {
      return this.http.put<Brand>(`${this.apiUrl}/${id}/reactivate`, {});
    }
}
