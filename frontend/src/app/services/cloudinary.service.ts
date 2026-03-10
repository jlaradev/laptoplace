import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SKIP_AUTH_INTERCEPTOR } from './auth.interceptor';

@Injectable({
  providedIn: 'root'
})
export class CloudinaryService {
  private cloudName = 'do3ub0chp';
  private uploadPreset = 'ml_default';

  constructor(private http: HttpClient) {}

  /**
   * Sube una imagen a Cloudinary directamente desde el cliente
   * Intenta con upload preset, si falla intenta sin preset (unsigned)
   */
  uploadImage(file: File, folder: string = ''): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    // No agregar preset - Cloudinary permite upload unsigned
    if (folder) {
      formData.append('folder', folder);
    }

    // Usar HttpContext para indicarle al interceptor que salte auth
    return this.http.post<any>(
      `https://api.cloudinary.com/v1_1/${this.cloudName}/image/upload`,
      formData,
      {
        context: new HttpContext().set(SKIP_AUTH_INTERCEPTOR, true)
      }
    );
  }
}
