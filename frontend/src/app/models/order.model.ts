export type OrderStatus = 'PENDIENTE_PAGO' | 'PROCESANDO' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO' | 'EXPIRADO';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface OrderPayment {
  id: number;
  estado: 'PENDIENTE' | 'COMPLETADO' | 'FALLIDO';
  amount: number;
  stripeId?: string;
  createdAt?: string;
}

export interface Order {
  id: number;
  userId: string;
  usuarioEmail: string;
  total: number;
  estado: OrderStatus;
  direccionEnvio: string;
  expiresAt: string | null;
  items: OrderItem[];
  payment: OrderPayment | null;
  createdAt: string;
}

export interface OrderPage {
  content: Order[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
}
