import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserResponseDTO {
  id: string;
  email: string;
  nombre: string;
  apellido: string;
  telefono?: string;
  direccion?: string;
  role?: 'USER' | 'ADMIN';
  createdAt?: string;
}

export type User = UserResponseDTO;

export interface UserPage {
  content: UserResponseDTO[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

export interface UserCreateDTO {
  email: string;
  password: string;
  nombre: string;
  apellido: string;
  telefono?: string;
  direccion?: string;
}

export interface UserUpdateDTO {
  nombre?: string;
  apellido?: string;
  telefono?: string;
  direccion?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = (window as any).APP_CONFIG?.apiBaseUrl
    ? `${(window as any).APP_CONFIG.apiBaseUrl.replace(/\/$/, '')}/api/users`
    : '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(page: number = 0, size: number = 20): Observable<UserPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<UserPage>(this.apiUrl, { params });
  }

  getUserById(id: string): Observable<UserResponseDTO> {
    return this.http.get<UserResponseDTO>(`${this.apiUrl}/${id}`);
  }

  getUserByEmail(email: string): Observable<UserResponseDTO> {
    return this.http.get<UserResponseDTO>(`${this.apiUrl}/email`, { params: { email } });
  }

  createUser(dto: UserCreateDTO): Observable<UserResponseDTO> {
    return this.http.post<UserResponseDTO>(`${this.apiUrl}/register`, dto);
  }

  updateUser(id: string, dto: UserUpdateDTO): Observable<UserResponseDTO> {
    return this.http.put<UserResponseDTO>(`${this.apiUrl}/${id}`, dto);
  }
    /**
     * Obtiene usuarios inactivos (eliminados lógicamente)
     */
    getInactiveUsers(page: number = 0, size: number = 20): Observable<UserPage> {
      const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());
      return this.http.get<UserPage>(`${this.apiUrl}/inactive`, { params });
    }

    /**
     * Desactiva (elimina lógicamente) un usuario
     */
    deactivateUser(id: string): Observable<void> {
      return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    /**
     * Reactiva un usuario eliminado
     */
    reactivateUser(id: string): Observable<UserResponseDTO> {
      return this.http.put<UserResponseDTO>(`${this.apiUrl}/${id}/reactivate`, {});
    }

    /**
     * Cambia el rol de un usuario (query param `role`, e.g. USER o ADMIN)
     */
    changeUserRole(id: string, role: 'USER' | 'ADMIN'): Observable<UserResponseDTO> {
      const params = new HttpParams().set('role', role);
      return this.http.patch<UserResponseDTO>(`${this.apiUrl}/${id}/role`, null, { params });
    }
}
