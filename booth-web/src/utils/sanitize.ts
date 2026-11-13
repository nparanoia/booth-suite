import DOMPurify from 'dompurify'

/** 商品详情 HTML 可能来自后台录入，展示前去掉脚本和事件属性。 */
export function sanitizeHtml(html: string | null | undefined) {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true },
  })
}
