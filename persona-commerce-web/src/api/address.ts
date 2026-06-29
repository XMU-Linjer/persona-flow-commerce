import http from './http'

export interface AddressItem {
  addressId: number
  recipientName: string
  recipientPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string
  isDefault: boolean
}

export interface AddressPayload {
  recipientName: string
  recipientPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  postalCode?: string
  isDefault: boolean
}

export function listAddresses() {
  return http.get<AddressItem[], AddressItem[]>('/api/users/me/addresses')
}

export function createAddress(data: AddressPayload) {
  return http.post<AddressItem, AddressItem>('/api/users/me/addresses', data)
}

export function updateAddress(addressId: number, data: AddressPayload) {
  return http.put<AddressItem, AddressItem>(`/api/users/me/addresses/${addressId}`, data)
}

export function deleteAddress(addressId: number) {
  return http.delete<void, void>(`/api/users/me/addresses/${addressId}`)
}

export function setDefaultAddress(addressId: number) {
  return http.put<AddressItem, AddressItem>(`/api/users/me/addresses/${addressId}/default`)
}
