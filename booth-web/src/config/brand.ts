export const BRAND = import.meta.env.VITE_BRAND || 'Booth'
export const BRAND_MARK = (BRAND.slice(0, 1) || 'B').toUpperCase()
/** 个人自营定位：短句可用于页脚 / 登录 / 首页 */
export const BRAND_TAGLINE = '个人自营小店'
export const BRAND_PITCH = '精选上架，店主直发；券、秒杀、售后都在站内完成。'
