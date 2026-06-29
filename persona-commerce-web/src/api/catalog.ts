import http from './http'

export interface PageResult<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export interface ProductListItem {
  spuId: number
  categoryId: number
  categoryName: string
  name: string
  subtitle?: string
  brand?: string
  mainImageUrl?: string
  minPrice?: number
  maxPrice?: number
  salesCount?: number
  tags?: string
  status?: number
}

export interface SkuItem {
  skuId: number
  skuName: string
  specs?: Record<string, unknown>
  price: number
  originalPrice?: number
  imageUrl?: string
  status?: number
  salesCount?: number
}

export interface ProductDetail {
  spuId: number
  categoryId: number
  categoryName: string
  name: string
  subtitle?: string
  brand?: string
  description?: string
  mainImageUrl?: string
  detailImages?: string[]
  attributes?: Record<string, unknown>
  tags?: string
  skus: SkuItem[]
}

export interface ProductSearchParams {
  categoryId?: number
  keyword?: string
  page?: number
  size?: number
}

export function listProducts(params: ProductSearchParams) {
  return http.get<PageResult<ProductListItem>, PageResult<ProductListItem>>(
    '/api/catalog/products',
    { params },
  )
}

export function getProductDetail(spuId: number) {
  return http.get<ProductDetail, ProductDetail>(`/api/catalog/products/${spuId}`)
}
