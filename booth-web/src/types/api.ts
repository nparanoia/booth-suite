export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
}

export interface TokenVO {
  token: string
  expiresIn: number
}

export interface MemberProfile {
  id: number
  username: string
  nickname: string | null
  phone: string
  avatar: string | null
  gender: string | null
  birthday: string | null
  integration: number
}

export interface Address {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  region: string
  detailAddress: string
  postCode: string | null
  isDefault: boolean | null
}

export interface CategoryTree {
  id: number
  parentId: number | null
  name: string
  level: number
  icon: string | null
  keywords: string | null
  sort: number
  navStatus: boolean | null
  showStatus: boolean | null
  children: CategoryTree[]
}

export interface ProductListItem {
  id: number
  productSn: string
  name: string
  subTitle: string | null
  pic: string | null
  price: number
  originalPrice: number | null
  sale: number
  publishStatus: string
  brandName: string | null
  categoryName: string | null
}

export interface SkuSpec {
  key: string
  value: string
}

export interface Sku {
  id: number
  skuCode: string
  specs: SkuSpec[] | null
  price: number
  pic: string | null
  availableStock: number | null
  sale: number | null
}

export interface ProductDetail {
  id: number
  categoryId: number
  brandId: number | null
  name: string
  subTitle: string | null
  pic: string | null
  albumPics: string[] | null
  detailHtml: string | null
  price: number
  originalPrice: number | null
  sale: number
  brandName: string | null
  categoryName: string | null
  params: { attrId: number; name: string; value: string }[] | null
  skus: Sku[]
}

export interface CartItem {
  id: number
  skuId: number
  productId: number
  productName: string
  pic: string | null
  specs: SkuSpec[] | null
  price: number
  quantity: number
  checked: boolean
  available: boolean
  availableStock: number
}

export interface Cart {
  items: CartItem[]
  checkedCount: number
  checkedAmount: number
}

export interface OrderItem {
  id: number | null
  skuId: number
  productName: string
  pic: string | null
  specs: SkuSpec[] | null
  price: number
  quantity: number
  realAmount: number | null
}

export interface OrderConfirm {
  submitToken: string
  items: OrderItem[]
  coupons: unknown[]
  integration: number
  maxUseIntegration: number
  goodsAmount: number
  freightAmount: number
  couponAmount: number
  integrationAmount: number
  payAmount: number
  useIntegration: number
}

export interface OrderSummary {
  orderSn: string
  status: string
  payAmount: number
  createTime: string
}

export interface OrderDetail {
  orderSn: string
  status: string
  totalAmount: number
  freightAmount: number
  couponAmount: number
  payAmount: number
  paymentStatus: string
  receiverName: string
  receiverPhone: string
  receiverProvince: string
  receiverCity: string
  receiverRegion: string
  receiverDetailAddress: string
  note: string | null
  deliveryCompany: string | null
  deliverySn: string | null
  createTime: string
  items: OrderItem[]
}
