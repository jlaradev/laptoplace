export interface Brand {
  id: number;
  nombre: string;
  descripcion: string;
  imageUrl: string | null;
  createdAt: string;
}

export interface BrandPage {
  content: Brand[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
}
