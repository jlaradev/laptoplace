import { Routes } from '@angular/router';
import { AdminGuard } from './guards/admin.guard';
import { AdminLayoutComponent } from './layout/admin-layout.component';
import { AdminLoginComponent } from './pages/admin-login.component';
import { AdminDashboardComponent } from './pages/admin-dashboard.component';
import { ProductsListComponent } from './pages/products/products-list.component';
import { ProductFormComponent } from './pages/products/product-form.component';
import { BrandsListComponent } from './pages/brands/brands-list.component';
import { BrandFormComponent } from './pages/brands/brand-form.component';
import { OrdersListComponent } from './pages/orders/orders-list.component';
import { OrderDetailComponent } from './pages/orders/order-detail.component';
import { UsersListComponent } from './pages/users/users-list.component';
import { UserDetailComponent } from './pages/users/user-detail.component';
import { UserFormComponent } from './pages/users/user-form.component';

export const adminRoutes: Routes = [
  {
    path: 'login',
    component: AdminLoginComponent
  },
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [AdminGuard],
    children: [
      {
        path: 'dashboard',
        component: AdminDashboardComponent
      },
      {
        path: 'products',
        component: ProductsListComponent
      },
      {
        path: 'products/create',
        component: ProductFormComponent
      },
      {
        path: 'products/edit/:id',
        component: ProductFormComponent
      },
      {
        path: 'brands',
        component: BrandsListComponent
      },
      {
        path: 'brands/create',
        component: BrandFormComponent
      },
      {
        path: 'brands/edit/:id',
        component: BrandFormComponent
      },
      {
        path: 'orders',
        component: OrdersListComponent
      },
      {
        path: 'orders/:id',
        component: OrderDetailComponent
      },
      {
        path: 'users',
        component: UsersListComponent
      },
      {
        path: 'users/create',
        component: UserFormComponent
      },
      {
        path: 'users/:id',
        component: UserDetailComponent
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  }
];
