import { getData } from './http'
import type { CategoryTree, PageResult, ProductDetail, ProductListItem } from '@/types/api'

export function fetchCategories() {
  return getData<CategoryTree[]>('/product/open/categories/tree')
}

export function fetchProducts(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  categoryId?: number
}) {
  return getData<PageResult<ProductListItem>>('/product/open/products', params)
}

export function fetchProduct(id: number) {
  return getData<ProductDetail>(`/product/open/products/${id}`)
}
