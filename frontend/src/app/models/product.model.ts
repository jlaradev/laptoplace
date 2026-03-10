import { Brand } from './brand.model';

export interface ProductImage {
  id: number;
  url: string;
  orden: number;
  descripcion?: string;
}

export interface Product {
  id: number;
  nombre: string;
  precio: number;
  stock: number;
  marca?: string;
  brandId?: number;
  brand?: Brand;  // El backend devuelve el objeto brand completo
  imagenPrincipal?: ProductImage;
  promedioRating: number;
  descripcion?: string;
  procesador?: string;
  ram?: number;
  almacenamiento?: number;
  pantalla?: string;
  gpu?: string;
  peso?: number;
}

export interface ProductPage {
  content: Product[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  empty: boolean;
}
