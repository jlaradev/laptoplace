import { Routes } from '@angular/router';
import { App } from './app';
import { LoginComponent } from './pages/login.component';
import { CatalogComponent } from './pages/catalog.component';
import { ProductDetailComponent } from './pages/product-detail.component';
import { CompareComponent } from './pages/compare.component';
import { CartPageComponent } from './pages/cart.component';
// No es necesario importar PaymentComponent para loadComponent

export const routes: Routes = [
  { path: '', component: App },
  { path: 'login', component: LoginComponent },
  { path: 'catalog', component: CatalogComponent },
  { path: 'product/:id', component: ProductDetailComponent },
  { path: 'compare', component: CompareComponent },
  { path: 'cart', component: CartPageComponent },
  { path: 'payment', loadComponent: () => import('./payment').then(m => m.PaymentComponent) },
  { path: 'profile', loadComponent: () => import('./pages/profile.component').then(m => m.ProfileComponent) },
  { path: '**', redirectTo: '' }
];
