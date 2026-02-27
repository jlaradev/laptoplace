import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: string;
  nombre: string;
  apellido: string;
  email: string;
  direccion: string;
  telefono?: string;
  role?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
    updateUser(userId: string, data: Partial<User>): Observable<User> {
      return this.http.put<User>(`${this.apiUrl}/${userId}`, data);
    }
  private apiUrl = (window as any).APP_CONFIG?.apiBaseUrl ? `${(window as any).APP_CONFIG.apiBaseUrl.replace(/\/$/, '')}/api/users` : '/api/users';

  constructor(private http: HttpClient) {}

  getUserById(userId: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${userId}`);
  }
}
