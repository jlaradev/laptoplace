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
  marca: string;
  imagenPrincipal?: ProductImage;
  promedioRating: number;
}

export interface ProductPage {
  content: Product[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  empty: boolean;
}
