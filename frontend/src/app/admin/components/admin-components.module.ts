import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductsEliminadosModalComponent } from './products-eliminados-modal.component';
import { BrandsEliminadosModalComponent } from './brands-eliminados-modal.component';
import { UsersEliminadosModalComponent } from './users-eliminados-modal.component';

@NgModule({
  imports: [ProductsEliminadosModalComponent, BrandsEliminadosModalComponent, UsersEliminadosModalComponent],
  exports: [ProductsEliminadosModalComponent, BrandsEliminadosModalComponent, UsersEliminadosModalComponent]
})
export class AdminComponentsModule {}
