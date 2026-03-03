export type OrderStatus = 'pendiente_pago' | 'procesando' | 'enviado' | 'entregado' | 'cancelado' | 'expirado';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Order {
  id: number;
  userId: string;
  total: number;
  estado: OrderStatus;
  direccionEnvio: string;
  expiresAt: string | null;
  items: OrderItem[];
  payment: {
    id: number;
    status: string;
    amount: number;
  } | null;
  createdAt: string;
}

export interface OrderPage {
  content: Order[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
}
