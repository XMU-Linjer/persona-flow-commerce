import http from './http'
import type { PageResult } from './catalog'

export interface FavoriteStatus {
  skuId: number
  favorited: boolean
}

export interface FavoriteItem {
  favoriteId: number
  skuId: number
  spuId: number
  categoryId: number
  categoryName: string
  productName: string
  skuName: string
  unitPrice: number
  imageUrl?: string
  createdAt: string
}

export interface AddCartItemRequest {
  skuId: number
  quantity: number
}

export interface UpdateCartItemRequest {
  quantity: number
}

export interface CartItemSimple {
  cartItemId: number
  skuId: number
  quantity: number
}

export interface CartItem {
  cartItemId: number
  skuId: number
  spuId: number
  categoryId: number
  categoryName: string
  productName: string
  skuName: string
  unitPrice: number
  imageUrl?: string
  quantity: number
  subtotal: number
  createdAt: string
  updatedAt: string
}

export function addFavorite(skuId: number) {
  return http.post<FavoriteStatus, FavoriteStatus>(`/api/shopping/favorites/${skuId}`)
}

export function removeFavorite(skuId: number) {
  return http.delete<FavoriteStatus, FavoriteStatus>(`/api/shopping/favorites/${skuId}`)
}

export function listFavorites(params: { page?: number; size?: number }) {
  return http.get<PageResult<FavoriteItem>, PageResult<FavoriteItem>>('/api/shopping/favorites', {
    params,
  })
}

export function addCartItem(data: AddCartItemRequest) {
  return http.post<CartItemSimple, CartItemSimple>('/api/shopping/cart/items', data)
}

export function updateCartItem(cartItemId: number, data: UpdateCartItemRequest) {
  return http.patch<CartItemSimple, CartItemSimple>(`/api/shopping/cart/items/${cartItemId}`, data)
}

export function deleteCartItem(cartItemId: number) {
  return http.delete<void, void>(`/api/shopping/cart/items/${cartItemId}`)
}

export function clearCart() {
  return http.delete<void, void>('/api/shopping/cart/items')
}

export function listCartItems() {
  return http.get<CartItem[], CartItem[]>('/api/shopping/cart/items')
}
