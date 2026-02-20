import { Routes } from '@angular/router';
import { App } from './app';
import { LoginComponent } from './pages/login.component';
import { CatalogComponent } from './pages/catalog.component';

export const routes: Routes = [
  { path: '', component: App },
  { path: 'login', component: LoginComponent },
  { path: 'catalog', component: CatalogComponent },
  { path: '**', redirectTo: '' }
];
