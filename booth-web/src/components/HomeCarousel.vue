<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { BRAND } from '@/config/brand'
import type { ProductListItem } from '@/types/api'
import { money, placeholderPic } from '@/utils/format'

export interface Slide {
  id: string
  tone: 'orange' | 'ink' | 'mint' | 'violet' | 'sand' | 'rose' | 'sky'
  eyebrow: string
  title: string
  desc: string
  cta: string
  to: string
  pic?: string | null
}

const props = defineProps<{
  products: ProductListItem[]
  couponHint?: string
  seckillHint?: string
}>()

const index = ref(0)
let timer: number | undefined

const slides = computed<Slide[]>(() => {
  const tops = props.products.slice(0, 3)
  const list: Slide[] = [
    {
      id: 'brand',
      tone: 'orange',
      eyebrow: `${BRAND} · 个人自营`,
      title: '本店好物，店主直发',
      desc: '精选上架，从逛到售后一条链路走完。',
      cta: '逛本店',
      to: '/category',
    },
    {
      id: 'coupon',
      tone: 'rose',
      eyebrow: '优惠券',
      title: props.couponHint || '领张券再下单更划算',
      desc: '本店满减券随到随领，结算时自动抵扣。',
      cta: '去领券',
      to: '/coupons',
    },
    {
      id: 'seckill',
      tone: 'ink',
      eyebrow: '限时秒杀',
      title: props.seckillHint || '整点开抢，手慢无',
      desc: '限时价库存不多，先到先得。',
      cta: '看秒杀',
      to: '/seckill',
    },
    {
      id: 'support',
      tone: 'mint',
      eyebrow: '店内客服',
      title: '查单、退货、取消付款',
      desc: '查单、退货、改地址可在对话里直接办。',
      cta: '问店员',
      to: '/support',
    },
  ]
  tops.forEach((p, i) => {
    list.push({
      id: `hot-${p.id}`,
      tone: (['violet', 'sand', 'sky'] as const)[i],
      eyebrow: i === 0 ? '店主主推' : '本店热卖',
      title: p.name,
      desc: p.subTitle || `到手价 ¥${money(p.price)} · ${p.sale || 0} 人已购`,
      cta: '去看看',
      to: `/product/${p.id}`,
      pic: p.pic || placeholderPic(p.id),
    })
  })
  while (list.length < 5) {
    list.push({
      id: `pad-${list.length}`,
      tone: 'sky',
      eyebrow: BRAND,
      title: '货架还在慢慢摆满',
      desc: '分类页里还有本店完整货架。',
      cta: '去分类',
      to: '/category',
    })
  }
  return list.slice(0, 7)
})

function go(i: number) {
  const n = slides.value.length
  index.value = ((i % n) + n) % n
}

function next() {
  go(index.value + 1)
}

function start() {
  stop()
  timer = window.setInterval(next, 4800)
}

function stop() {
  if (timer) window.clearInterval(timer)
}

watch(slides, () => {
  index.value = 0
  start()
})

onMounted(start)
onUnmounted(stop)
</script>

<template>
  <section class="carousel" @mouseenter="stop" @mouseleave="start">
    <div class="track" :style="{ transform: `translateX(-${index * 100}%)` }">
      <article
        v-for="s in slides"
        :key="s.id"
        class="slide"
        :class="s.tone"
      >
        <div class="inner">
          <div class="copy">
            <p class="eye">{{ s.eyebrow }}</p>
            <h2>{{ s.title }}</h2>
            <p class="desc">{{ s.desc }}</p>
            <RouterLink class="btn" :to="s.to">{{ s.cta }}</RouterLink>
          </div>
          <div class="art" aria-hidden="true">
            <img v-if="s.pic" class="product" :src="s.pic" alt="" />
            <div v-else class="blob" />
          </div>
        </div>
      </article>
    </div>
    <div class="dots">
      <button
        v-for="(s, i) in slides"
        :key="s.id"
        type="button"
        :class="{ on: i === index }"
        :aria-label="`第 ${i + 1} 张`"
        @click="go(i)"
      />
    </div>
    <button class="nav prev" type="button" aria-label="上一张" @click="go(index - 1)">‹</button>
    <button class="nav next" type="button" aria-label="下一张" @click="go(index + 1)">›</button>
  </section>
</template>

<style scoped>
.carousel {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}
.track {
  display: flex;
  height: 100%;
  transition: transform .55s cubic-bezier(.22, 1, .36, 1);
}
.slide {
  flex: 0 0 100%;
  height: 100%;
  min-height: 100%;
}
.inner {
  display: grid;
  grid-template-columns: 1.1fr .9fr;
  align-items: center;
  gap: 20px;
  height: 100%;
  padding: 36px 48px 48px 40px;
  box-sizing: border-box;
}
.copy { max-width: 28rem; }
.eye {
  margin: 0 0 8px;
  font-weight: 700;
  letter-spacing: .12em;
  text-transform: uppercase;
  font-size: .78rem;
  opacity: .88;
}
h2 {
  margin: 0;
  font-size: clamp(1.6rem, 2.8vw, 2.45rem);
  line-height: 1.15;
  letter-spacing: -.02em;
  font-weight: 700;
}
.desc {
  margin: 12px 0 20px;
  font-size: .98rem;
  opacity: .86;
  max-width: 24rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.art {
  display: grid;
  place-items: center;
  min-height: 220px;
}
.blob {
  width: min(100%, 260px);
  aspect-ratio: 1;
  border-radius: 42% 58% 50% 50%;
  background: rgba(255,255,255,.22);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.18);
  animation: float 7s ease-in-out infinite;
}
.product {
  width: min(100%, 260px);
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 20px;
  box-shadow: 0 18px 48px rgba(0,0,0,.2);
  background: rgba(255,255,255,.35);
}
.orange { background: linear-gradient(135deg, #ff8a3d, #ff6700 55%, #c2410c); color: #fff; }
.ink { background: linear-gradient(135deg, #1f2937, #111827 60%, #000); color: #fff; }
.mint { background: linear-gradient(135deg, #34d399, #0f766e 60%, #115e59); color: #fff; }
.violet { background: linear-gradient(135deg, #a78bfa, #6d28d9 55%, #4c1d95); color: #fff; }
.sand { background: linear-gradient(135deg, #fde68a, #f59e0b 50%, #b45309); color: #1c1917; }
.rose { background: linear-gradient(135deg, #fb7185, #e11d48 55%, #9f1239); color: #fff; }
.sky { background: linear-gradient(135deg, #7dd3fc, #0284c7 55%, #075985); color: #fff; }
.orange .btn, .ink .btn, .mint .btn, .violet .btn, .rose .btn, .sky .btn {
  background: #fff;
  color: #111;
}
.sand .btn { background: #111; color: #fff; }
.dots {
  position: absolute;
  right: 28px;
  bottom: 18px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.dots button {
  width: 8px; height: 8px; border-radius: 999px; border: 0;
  background: rgba(255,255,255,.45);
  padding: 0;
}
.dots button.on { width: 22px; background: #fff; }
.nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px; height: 36px;
  border-radius: 50%;
  border: 0;
  background: rgba(255,255,255,.72);
  font-size: 1.3rem;
  line-height: 1;
  color: #111;
  opacity: 0;
  transition: opacity .2s;
}
.carousel:hover .nav { opacity: 1; }
.nav.prev { left: 12px; }
.nav.next { right: 12px; }
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
@media (max-width: 900px) {
  .slide { min-height: 240px; }
  .inner { grid-template-columns: 1fr; padding: 22px 20px 40px; }
  .art { display: none; }
  .nav { display: none; }
  .dots { left: 0; right: 0; justify-content: center; }
}
</style>
