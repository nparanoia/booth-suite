<script setup lang="ts">
import { BRAND, BRAND_PITCH, BRAND_TAGLINE } from '@/config/brand'
const brand = BRAND
</script>

<template>
  <footer class="footer">
    <div class="container grid">
      <div>
        <strong>{{ brand }}</strong>
        <p class="muted">{{ BRAND_TAGLINE }}</p>
        <p class="muted pitch">{{ BRAND_PITCH }}</p>
      </div>
      <div>
        <h4>逛本店</h4>
        <RouterLink to="/coupons">领优惠券</RouterLink>
        <RouterLink to="/seckill">限时秒杀</RouterLink>
      </div>
      <div>
        <h4>买后服务</h4>
        <RouterLink to="/support">店内客服</RouterLink>
        <button type="button" class="dead" disabled>售后说明</button>
      </div>
      <div>
        <h4>关于{{ brand }}</h4>
        <button type="button" class="dead" disabled>个人自营小店</button>
        <button type="button" class="dead" disabled>品牌故事</button>
      </div>
    </div>
    <p class="copy muted">© {{ new Date().getFullYear() }} {{ brand }} · 个人自营 Demo</p>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 56px;
  background: #fff;
  border-top: 1px solid var(--line);
  padding: 40px 0 24px;
}
.grid {
  display: grid;
  grid-template-columns: 1.4fr repeat(3, 1fr);
  gap: 24px;
}
h4 { margin: 0 0 12px; font-size: .95rem; }
a, .dead {
  display: block;
  margin: 0 0 8px;
  color: var(--muted);
  background: none;
  border: 0;
  padding: 0;
  text-align: left;
}
a:hover { color: var(--brand); }
.dead { cursor: default; opacity: .7; }
.pitch { margin-top: 8px; max-width: 28ch; font-size: .88rem; }
.copy { text-align: center; margin: 28px 0 0; font-size: .85rem; }
@media (max-width: 768px) {
  .grid { grid-template-columns: 1fr 1fr; }
}
</style>
