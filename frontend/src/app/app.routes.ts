import { Routes } from '@angular/router';
import { App } from './app';
import { LoginComponent } from './pages/login.component';
import { CatalogComponent } from './pages/catalog.component';
import { ProductDetailComponent } from './pages/product-detail.component';
import { CompareComponent } from './pages/compare.component';

export const routes: Routes = [
  { path: '', component: App },
  { path: 'login', component: LoginComponent },
  { path: 'catalog', component: CatalogComponent },
  { path: 'product/:id', component: ProductDetailComponent },
  { path: 'compare', component: CompareComponent },
  { path: '**', redirectTo: '' }
];
