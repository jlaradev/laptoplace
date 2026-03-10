import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const token = this.authService.getToken();
    const role = localStorage.getItem('role');

    // Verificar si hay token y si el rol es ADMIN
    if (token && role === 'ADMIN') {
      return true;
    }

    // Si es usuario regular (USER)
    if (token && role === 'USER') {
      this.router.navigate(['/']);
      return false;
    }

    // Si no está autenticado
    if (!token) {
      this.router.navigate(['/admin/login'], {
        queryParams: { returnUrl: state.url }
      });
      return false;
    }

    // Fallback
    this.router.navigate(['/']);
    return false;
  }
}
