import http from './http'
import type { PageResult } from './catalog'

export interface CreateOrderItemRequest {
  skuId: number
  quantity: number
}

export interface CreateOrderRequest {
  addressId: number
  items: CreateOrderItemRequest[]
}

export interface OrderItem {
  skuId: number
  spuId: number
  categoryId: number
  categoryName: string
  productName: string
  skuName: string
  imageUrl?: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface OrderCreateResult {
  orderId: number
  orderNo: string
  totalAmount: number
  status: number
  createdAt: string
  items: OrderItem[]
}

export interface OrderListItem {
  orderId: number
  orderNo: string
  totalAmount: number
  status: number
  createdAt: string
  paidAt?: string
  canceledAt?: string
}

export interface OrderAddress {
  addressId: number
  recipientName: string
  recipientPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string
}

export interface PaymentInfo {
  paymentNo: string
  orderId: number
  orderNo: string
  amount: number
  channel: string
  status: number
  paidAt: string
}

export interface OrderDetail {
  orderId: number
  orderNo: string
  userId: number
  address: OrderAddress
  totalAmount: number
  status: number
  createdAt: string
  paidAt?: string
  canceledAt?: string
  items: OrderItem[]
  payment?: PaymentInfo | null
}

export interface OrderStatusResult {
  orderId: number
  orderNo: string
  status: number
  canceledAt?: string
}

export function createOrder(data: CreateOrderRequest) {
  return http.post<OrderCreateResult, OrderCreateResult>('/api/trade/orders', data)
}

export function listOrders(params: { status?: number; page?: number; size?: number }) {
  return http.get<PageResult<OrderListItem>, PageResult<OrderListItem>>('/api/trade/orders', {
    params,
  })
}

export function getOrderDetail(orderId: number) {
  return http.get<OrderDetail, OrderDetail>(`/api/trade/orders/${orderId}`)
}

export function cancelOrder(orderId: number) {
  return http.post<OrderStatusResult, OrderStatusResult>(`/api/trade/orders/${orderId}/cancel`)
}

export function payOrder(orderId: number) {
  return http.post<PaymentInfo, PaymentInfo>(`/api/trade/orders/${orderId}/pay`, {
    channel: 'MOCK',
  })
}
