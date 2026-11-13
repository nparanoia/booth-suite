<script setup lang="ts">
import { computed } from 'vue'
import type { ProductListItem } from '@/types/api'
import { money, placeholderPic } from '@/utils/format'

const props = withDefaults(defineProps<{
  product: ProductListItem
  dense?: boolean
}>(), {
  dense: false,
})
const pic = computed(() => props.product.pic || placeholderPic(props.product.id))
</script>

<template>
  <RouterLink class="item" :class="{ dense }" :to="`/product/${product.id}`">
    <div class="pic-wrap">
      <img :src="pic" :alt="product.name" loading="lazy" />
    </div>
    <div class="body">
      <h3>{{ product.name }}</h3>
      <p v-if="product.subTitle && !dense" class="sub">{{ product.subTitle }}</p>
      <div class="row">
        <span class="price"><span class="yen">¥</span>{{ money(product.price) }}</span>
        <span class="muted sale">{{ product.sale || 0 }}人付款</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.item {
  display: flex;
  flex-direction: column;
  background: var(--card);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: transform .2s, box-shadow .2s;
}
.item:hover { transform: translateY(-3px); box-shadow: 0 14px 36px rgba(25,25,25,.12); }
.pic-wrap {
  aspect-ratio: 1;
  background: #fafafa;
  overflow: hidden;
}
.pic-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.body { padding: 14px 14px 16px; }
h3 {
  margin: 0;
  font-size: .98rem;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.8em;
}
.sub {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: .82rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.sale { font-size: .78rem; }

.item.dense {
  border-radius: 8px;
  box-shadow: 0 1px 0 rgba(28, 25, 23, 0.04), 0 8px 20px rgba(28, 25, 23, 0.05);
}
.item.dense .pic-wrap {
  aspect-ratio: 1.05;
}
.item.dense .body {
  padding: 8px 10px 10px;
}
.item.dense h3 {
  font-size: .86rem;
  min-height: 0;
  -webkit-line-clamp: 1;
  line-height: 1.35;
}
.item.dense .row {
  margin-top: 6px;
}
.item.dense .price {
  font-size: 1rem;
}
.item.dense .sale {
  font-size: .72rem;
}
</style>
