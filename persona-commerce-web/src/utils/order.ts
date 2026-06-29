export const ORDER_STATUS_PENDING = 10
export const ORDER_STATUS_PAID = 20
export const ORDER_STATUS_CANCELED = 30

export function orderStatusLabel(status?: number) {
  if (status === ORDER_STATUS_PENDING) return '待支付'
  if (status === ORDER_STATUS_PAID) return '已支付'
  if (status === ORDER_STATUS_CANCELED) return '已取消'
  return '未知'
}

export function orderStatusTagType(status?: number) {
  if (status === ORDER_STATUS_PENDING) return 'warning'
  if (status === ORDER_STATUS_PAID) return 'success'
  if (status === ORDER_STATUS_CANCELED) return 'info'
  return 'danger'
}

export function formatMoney(value?: number) {
  return value == null ? '￥0.00' : `￥${Number(value).toFixed(2)}`
}

export function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
