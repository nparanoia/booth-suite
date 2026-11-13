<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { useAdminStore } from '@/stores/admin'
import { useUiStore } from '@/stores/ui'

interface OpsSummary {
  day: string
  chats: number
  policyHits: number
  policyMisses: number
  writeReturns: number
  writeCancels: number
  writeFails: number
  toolErrors: number
  tools: Record<string, number>
  writeCircuitOpen?: boolean
  changeAddressEnabled?: boolean
  humanTransferEnabled?: boolean
  humanPhone?: string
  chatRetentionDays?: number
}

interface TranscriptRow {
  conversation_id: string
  member_hash: string
  role: string
  content: string
  intent: string | null
  order_sn: string | null
  tools: string | null
  created_at: string
}

interface KnowledgeRow {
  id: number
  source: string
  heading: string
  content_preview: string
  valid_from: string | null
  valid_to: string | null
}

const admin = useAdminStore()
const ui = useUiStore()
const agentBase = import.meta.env.VITE_AGENT_BASE || '/agent'
const data = ref<OpsSummary | null>(null)
const error = ref('')
const loading = ref(true)
const bars = ref<{ label: string; value: number; height: string }[]>([])

const chatQuery = ref({ conversationId: '', memberHash: '', orderSn: '' })
const chats = ref<TranscriptRow[]>([])
const knowledge = ref<KnowledgeRow[]>([])
const kbForm = ref({
  source: 'ops-manual.md',
  heading: '',
  content: '',
  validFrom: '',
  validTo: '',
})

function headers() {
  return { Authorization: `Bearer ${admin.token}` }
}

async function agentGet<T>(path: string, params?: Record<string, string>) {
  const res = await axios.get(`${agentBase}${path}`, { headers: headers(), params })
  if (res.data && typeof res.data.code === 'number' && res.data.code !== 0) {
    throw new Error(res.data.message || '请求失败')
  }
  return (res.data?.data ?? res.data) as T
}

async function agentPost(path: string, body: unknown) {
  const res = await axios.post(`${agentBase}${path}`, body, { headers: headers() })
  if (res.data && typeof res.data.code === 'number' && res.data.code !== 0) {
    throw new Error(res.data.message || '请求失败')
  }
  return res.data?.data ?? res.data
}

async function agentDelete(path: string, params: Record<string, string>) {
  const res = await axios.delete(`${agentBase}${path}`, { headers: headers(), params })
  if (res.data && typeof res.data.code === 'number' && res.data.code !== 0) {
    throw new Error(res.data.message || '请求失败')
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (!admin.token) throw new Error('请先管理员登录')
    const body = await agentGet<OpsSummary>('/app/ops/summary')
    data.value = body
    const items = [
      { label: '对话', value: body.chats || 0 },
      { label: '政策命中', value: body.policyHits || 0 },
      { label: '政策拒答', value: body.policyMisses || 0 },
      { label: '写操作·退货', value: body.writeReturns || 0 },
      { label: '写操作·取消', value: body.writeCancels || 0 },
      { label: '写操作·失败', value: body.writeFails || 0 },
      { label: '工具错误', value: body.toolErrors || 0 },
    ]
    const max = Math.max(1, ...items.map((i) => i.value))
    bars.value = items.map((i) => ({
      ...i,
      height: `${Math.round((i.value / max) * 100)}%`,
    }))
    knowledge.value = await agentGet<KnowledgeRow[]>('/app/ops/knowledge', { limit: '50' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '无法连接 booth-agent'
    ui.toast(error.value)
  } finally {
    loading.value = false
  }
}

async function searchChats() {
  try {
    const params: Record<string, string> = { limit: '100' }
    if (chatQuery.value.conversationId.trim()) params.conversationId = chatQuery.value.conversationId.trim()
    if (chatQuery.value.memberHash.trim()) params.memberHash = chatQuery.value.memberHash.trim()
    if (chatQuery.value.orderSn.trim()) params.orderSn = chatQuery.value.orderSn.trim()
    chats.value = await agentGet<TranscriptRow[]>('/app/ops/chats', params)
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '查询失败')
  }
}

async function saveKnowledge() {
  try {
    await agentPost('/app/ops/knowledge', {
      source: kbForm.value.source,
      heading: kbForm.value.heading,
      content: kbForm.value.content,
      validFrom: kbForm.value.validFrom ? new Date(kbForm.value.validFrom).toISOString() : null,
      validTo: kbForm.value.validTo ? new Date(kbForm.value.validTo).toISOString() : null,
    })
    ui.toast('知识已写入')
    kbForm.value.heading = ''
    kbForm.value.content = ''
    knowledge.value = await agentGet<KnowledgeRow[]>('/app/ops/knowledge', { limit: '50' })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '保存失败')
  }
}

async function removeKnowledge(source: string) {
  if (!window.confirm(`确认删除知识来源「${source}」？此操作不可撤销。`)) {
    return
  }
  try {
    await agentDelete('/app/ops/knowledge', { source })
    ui.toast('已删除')
    knowledge.value = await agentGet<KnowledgeRow[]>('/app/ops/knowledge', { limit: '50' })
  } catch (e) {
    ui.toast(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1 style="margin:0 0 16px;font-size:1.35rem">客服助手</h1>
    <p class="muted" style="margin-top:-8px">
      需管理员 Token · 对话取证保留约 {{ data?.chatRetentionDays || '—' }} 天 · 店主电话 {{ data?.humanPhone || '—' }}
      · 写熔断 {{ data?.writeCircuitOpen ? '开' : '关' }}
      · 改地址 {{ data?.changeAddressEnabled ? '开' : '关' }}
      · 转人工 {{ data?.humanTransferEnabled ? '开' : '关' }}
    </p>
    <div class="admin-toolbar">
      <button class="btn" type="button" @click="load">刷新</button>
    </div>
    <div v-if="loading" class="empty">加载中…</div>
    <div v-else-if="error" class="admin-card">
      <p>{{ error }}</p>
      <p class="muted">请确认已管理员登录，且 booth-agent、商城网关可用。</p>
    </div>
    <template v-else>
      <div class="stat-grid">
        <div v-for="b in bars" :key="b.label" class="stat">
          <div class="n">{{ b.value }}</div>
          <div class="l">{{ b.label }}</div>
        </div>
      </div>
      <section class="admin-card">
        <h2 style="margin:0 0 8px;font-size:1rem">今日指标</h2>
        <div class="bars">
          <div v-for="b in bars" :key="b.label" class="bar-col">
            <div class="bar" :style="{ height: b.height }" />
            <span>{{ b.label }}</span>
          </div>
        </div>
      </section>
      <section class="admin-card" style="margin-top:14px">
        <h2 style="margin:0 0 12px;font-size:1rem">工具调用次数</h2>
        <table class="admin-table">
          <thead><tr><th>工具</th><th>次数</th></tr></thead>
          <tbody>
            <tr v-for="(n, name) in (data?.tools || {})" :key="name">
              <td>{{ name }}</td>
              <td>{{ n }}</td>
            </tr>
            <tr v-if="!Object.keys(data?.tools || {}).length">
              <td colspan="2" class="muted">暂无</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="admin-card" style="margin-top:14px">
        <h2 style="margin:0 0 12px;font-size:1rem">对话取证</h2>
        <div class="admin-toolbar" style="margin-bottom:10px">
          <input v-model="chatQuery.conversationId" placeholder="conversationId" />
          <input v-model="chatQuery.memberHash" placeholder="memberHash" />
          <input v-model="chatQuery.orderSn" placeholder="订单号" />
          <button class="btn" type="button" @click="searchChats">查询</button>
        </div>
        <table class="admin-table">
          <thead>
            <tr><th>时间</th><th>角色</th><th>内容</th><th>意图</th><th>单号</th></tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in chats" :key="i">
              <td class="muted">{{ row.created_at }}</td>
              <td>{{ row.role }}</td>
              <td style="white-space:pre-wrap;max-width:360px">{{ row.content }}</td>
              <td>{{ row.intent || '—' }}</td>
              <td>{{ row.order_sn || '—' }}</td>
            </tr>
            <tr v-if="!chats.length"><td colspan="5" class="muted">输入条件后查询</td></tr>
          </tbody>
        </table>
      </section>

      <section class="admin-card" style="margin-top:14px">
        <h2 style="margin:0 0 12px;font-size:1rem">知识 / 活动补库</h2>
        <div class="kb-form">
          <input v-model="kbForm.source" placeholder="source 如 activity-618.md" />
          <input v-model="kbForm.heading" placeholder="标题" />
          <textarea v-model="kbForm.content" rows="3" placeholder="正文说明" />
          <div class="admin-toolbar">
            <label class="muted">生效起 <input v-model="kbForm.validFrom" type="datetime-local" /></label>
            <label class="muted">生效止 <input v-model="kbForm.validTo" type="datetime-local" /></label>
            <button class="btn" type="button" @click="saveKnowledge">写入知识库</button>
          </div>
        </div>
        <table class="admin-table" style="margin-top:12px">
          <thead>
            <tr><th>source</th><th>标题</th><th>预览</th><th>时效</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="k in knowledge" :key="k.id">
              <td>{{ k.source }}</td>
              <td>{{ k.heading }}</td>
              <td class="muted">{{ k.content_preview }}</td>
              <td class="muted">{{ k.valid_from || '—' }} ~ {{ k.valid_to || '—' }}</td>
              <td><button class="btn ghost" type="button" @click="removeKnowledge(k.source)">删 source</button></td>
            </tr>
            <tr v-if="!knowledge.length"><td colspan="5" class="muted">暂无自定义知识</td></tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>

<style scoped>
.kb-form {
  display: grid;
  gap: 8px;
}
.kb-form textarea,
.kb-form input {
  width: 100%;
  box-sizing: border-box;
}
</style>
