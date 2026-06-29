const CHECKOUT_ITEMS_KEY = 'persona_commerce_checkout_items'

export interface CheckoutItem {
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

export function saveCheckoutItems(items: CheckoutItem[]) {
  sessionStorage.setItem(CHECKOUT_ITEMS_KEY, JSON.stringify(items))
}

export function loadCheckoutItems() {
  const raw = sessionStorage.getItem(CHECKOUT_ITEMS_KEY)
  if (!raw) {
    return []
  }

  try {
    const items = JSON.parse(raw) as CheckoutItem[]
    return Array.isArray(items) ? items : []
  } catch {
    sessionStorage.removeItem(CHECKOUT_ITEMS_KEY)
    return []
  }
}

export function clearCheckoutItems() {
  sessionStorage.removeItem(CHECKOUT_ITEMS_KEY)
}
