import type {
  AuthResponse,
  Cart,
  CartItem,
  Category,
  CurrentUserResponse,
  Order,
  Product,
  ProductPage,
  ProductPayload,
  UserProfile
} from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const AUTH_STORAGE_KEY = 'cool-online-shop.auth';

export class ApiError extends Error {
  status: number;
  details: unknown;

  constructor(status: number, message: string, details: unknown) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

export function getStoredAuth(): AuthResponse | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function setStoredAuth(auth: AuthResponse | null) {
  if (auth) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  } else {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const auth = getStoredAuth();
  const headers = new Headers(init.headers);

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (auth?.accessToken) {
    headers.set('Authorization', `Bearer ${auth.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message =
      typeof data === 'object' && data && 'message' in data
        ? String((data as { message: unknown }).message)
        : `Request failed with status ${response.status}`;
    throw new ApiError(response.status, message, data);
  }

  return data as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),

  register: (email: string, password: string) =>
    request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    }),

  me: () => request<CurrentUserResponse>('/auth/me'),

  getProducts: (page = 0, size = 20) =>
    request<ProductPage>(`/catalog/products?page=${page}&size=${size}`),

  getProduct: (id: number) => request<Product>(`/catalog/products/${id}`),

  createProduct: (payload: ProductPayload) =>
    request<Product>('/catalog/products', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  updateProduct: (id: number, payload: ProductPayload) =>
    request<Product>(`/catalog/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),

  deleteProduct: (id: number) =>
    request<void>(`/catalog/products/${id}`, {
      method: 'DELETE'
    }),

  getCategories: () => request<Category[]>('/catalog/categories'),

  createCategory: (name: string, description: string) =>
    request<Category>('/catalog/categories', {
      method: 'POST',
      body: JSON.stringify({ name, description })
    }),

  getCart: () => request<Cart>('/cart'),

  addCartItem: (productId: number, quantity: number) =>
    request<CartItem>('/cart/items', {
      method: 'POST',
      body: JSON.stringify({ productId, quantity })
    }),

  updateCartItem: (productId: number, quantity: number) =>
    request<CartItem>(`/cart/items/${productId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity })
    }),

  deleteCartItem: (productId: number) =>
    request<void>(`/cart/items/${productId}`, {
      method: 'DELETE'
    }),

  clearCart: () =>
    request<void>('/cart', {
      method: 'DELETE'
    }),

  checkout: () =>
    request<Order>('/orders/checkout', {
      method: 'POST'
    }),

  getOrders: () => request<Order[]>('/orders'),

  getOrder: (id: number) => request<Order>(`/orders/${id}`),

  getProfile: () => request<UserProfile>('/users/me'),

  createProfile: (payload: { firstName: string; lastName: string; phone: string }) =>
    request<UserProfile>('/users/me', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),

  updateProfile: (payload: { firstName: string; lastName: string; phone: string }) =>
    request<UserProfile>('/users/me', {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
};
