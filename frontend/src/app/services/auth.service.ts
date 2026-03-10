import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
      registerUser(data: { email: string; password: string; nombre: string; apellido: string; telefono?: string; direccion?: string }): Observable<any> {
        return this.http.post<any>('https://laptophub-cigv.onrender.com/api/users/register', data);
      }
    resetPassword(token: string, password: string): Observable<any> {
      // Backend expects { token, newPassword }
      return this.http.post<any>(`${this.apiBaseUrl}/reset-password`, { token, newPassword: password });
    }
  private apiBaseUrl = 'https://laptophub-cigv.onrender.com/api/auth';
  private isLoggedIn$ = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiBaseUrl}/login`, request).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('userId', response.userId);
        localStorage.setItem('email', response.email);
        localStorage.setItem('role', response.role);
        this.isLoggedIn$.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    this.isLoggedIn$.next(false);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getIsLoggedIn(): Observable<boolean> {
    return this.isLoggedIn$.asObservable();
  }

  isLoggedInSync(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  getUserEmail(): string | null {
    return localStorage.getItem('email');
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

    forgotPassword(email: string): Observable<any> {
      return this.http.post<any>(`${this.apiBaseUrl}/forgot-password`, { email });
    }
}
