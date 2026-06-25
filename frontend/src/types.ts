export type Role = 'USER' | 'ADMIN';

export interface AuthResponse {
  userId: number;
  email: string;
  role: Role;
  accessToken: string;
}

export interface CurrentUserResponse {
  userId: number;
  email: string;
  role: Role;
}

export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  categoryId: number;
  availableQuantity: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPage {
  items: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface CartItem {
  userId: number;
  productId: number;
  quantity: number;
}

export interface Cart {
  userId: number;
  items: CartItem[];
}

export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED' | 'COMPLETED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  quantity: number;
}

export interface Order {
  id: number;
  userId: number;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}

export interface UserProfile {
  id: number;
  authUserId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPayload {
  name: string;
  description: string;
  price: number;
  categoryId: number;
  availableQuantity: number;
}
