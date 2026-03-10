import { Routes } from '@angular/router';
import { App } from './app';
import { LoginComponent } from './pages/login.component';
import { CatalogComponent } from './pages/catalog.component';
import { ProductDetailComponent } from './pages/product-detail.component';
import { CompareComponent } from './pages/compare.component';
import { CartPageComponent } from './pages/cart.component';
import { adminRoutes } from './admin/admin.routes';
// No es necesario importar PaymentComponent para loadComponent

export const routes: Routes = [
  { path: '', component: App },
  { path: 'login', component: LoginComponent },
  { path: 'register', loadComponent: () => import('./pages/register.component').then(m => m.RegisterComponent) },
  { path: 'reset-password', loadComponent: () => import('./pages/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'catalog', component: CatalogComponent },
  { path: 'product/:id', component: ProductDetailComponent },
  { path: 'product/:productId/review', loadComponent: () => import('./pages/create-review.component').then(m => m.CreateReviewComponent) },
  { path: 'my-reviews', loadComponent: () => import('./pages/my-reviews.component').then(m => m.MyReviewsComponent) },
  { path: 'compare', component: CompareComponent },
  { path: 'cart', component: CartPageComponent },
  { path: 'payment', loadComponent: () => import('./payment').then(m => m.PaymentComponent) },
  { path: 'profile', loadComponent: () => import('./pages/profile.component').then(m => m.ProfileComponent) },
  { path: 'orders', loadComponent: () => import('./pages/orders.component').then(m => m.OrdersComponent) },
  { path: 'admin', children: adminRoutes },
  { path: '**', redirectTo: '' }
];
