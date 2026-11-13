<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  agentErrorMessage,
  cancelChatRun,
  postChatStream,
  postChatSync,
  type AgentChatBody,
  type AgentChatPayload,
} from '@/api/agent'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'
import { BRAND } from '@/config/brand'
import { fetchOrders } from '@/api/trade'
import type { OrderSummary } from '@/types/api'
import { orderStatusLabel } from '@/utils/format'

interface Msg {
  role: 'user' | 'bot' | 'sys'
  text: string
  sources?: string[]
  sourcesOpen?: boolean
}

const CID_KEY = 'ql_support_cid'
const route = useRoute()
const router = useRouter()
const user = useUserStore()
const ui = useUiStore()
const conversationId = ref(localStorage.getItem(CID_KEY) || crypto.randomUUID().replace(/-/g, ''))
const input = ref('')
const busy = ref(false)
const runId = ref('')
const streamHint = ref('')
const messages = ref<Msg[]>([
  {
    role: 'sys',
    text: `${BRAND} 店内客服：可查订单、问政策、退货/取消/改地址。从订单页进入或点选下方订单可带单。退货/取消/改地址在对话中直接办理（无确认卡）。要人工请点「店主电话」。`,
  },
])
const logEl = ref<HTMLElement | null>(null)
const recentOrders = ref<OrderSummary[]>([])
let abort: AbortController | null = null

localStorage.setItem(CID_KEY, conversationId.value)

async function scrollBottom() {
  await nextTick()
  if (logEl.value) logEl.value.scrollTop = logEl.value.scrollHeight
}

function requireLogin(): boolean {
  if (user.token) return true
  ui.toast('请先登录后再使用客服')
  void router.push({ name: 'login', query: { redirect: '/support' } })
  return false
}

async function loadOrders() {
  if (!user.token) return
  try {
    const page = await fetchOrders({ pageNum: 1, pageSize: 8 })
    recentOrders.value = page.list || []
  } catch {
    recentOrders.value = []
    messages.value.push({ role: 'sys', text: '最近订单加载失败，仍可手动输入订单号办理。' })
  }
}

function applyDone(body: AgentChatBody, botIndex: number) {
  if (body.conversationId) {
    conversationId.value = body.conversationId
    localStorage.setItem(CID_KEY, conversationId.value)
  }
  const text = body.reply || messages.value[botIndex]?.text || '（无回复）'
  messages.value[botIndex] = {
    role: 'bot',
    text,
    sources: body.sources?.length ? [...body.sources] : undefined,
    sourcesOpen: false,
  }
}

/** 主路径 SSE；未建立流前失败则降级同步。 */
async function runChat(payload: AgentChatPayload, botIndex: number): Promise<void> {
  if (!user.token) return
  streamHint.value = ''
  runId.value = ''
  abort?.abort()

  let started = false
  let finished = false
  let acc = ''

  await new Promise<void>((resolve, reject) => {
    abort = postChatStream(user.token, payload, {
      onStarted: (id) => {
        started = true
        runId.value = id
      },
      onProgress: (text) => {
        streamHint.value = text
        void scrollBottom()
      },
      onToken: (text) => {
        acc += text
        messages.value[botIndex] = { role: 'bot', text: acc || '…' }
        void scrollBottom()
      },
      onDone: (body) => {
        finished = true
        streamHint.value = ''
        applyDone(body, botIndex)
        resolve()
      },
      onCancelled: () => {
        finished = true
        streamHint.value = ''
        if (!messages.value[botIndex]?.text || messages.value[botIndex].text === '…') {
          messages.value[botIndex] = { role: 'bot', text: '已取消本轮生成' }
        }
        resolve()
      },
      onError: (message) => {
        if (finished) return
        if (started) {
          finished = true
          streamHint.value = ''
          messages.value[botIndex] = { role: 'bot', text: message }
          reject(new Error(message))
          return
        }
        void postChatSync(user.token, payload)
          .then((body) => {
            finished = true
            streamHint.value = '已降级为同步回复'
            applyDone(body, botIndex)
            resolve()
          })
          .catch((error) => {
            finished = true
            streamHint.value = ''
            const msg = agentErrorMessage(error)
            messages.value[botIndex] = { role: 'bot', text: msg }
            reject(new Error(msg))
          })
      },
    })
  })
}

async function bindEntryOrder() {
  const orderSn = String(route.query.orderSn || '').trim()
  if (!orderSn || !user.token) return
  const intent = String(route.query.intent || '').trim()
  busy.value = true
  const botIndex = messages.value.push({ role: 'bot', text: '…' }) - 1
  try {
    await runChat(
      {
        message: '',
        conversationId: conversationId.value,
        contextOrderSn: orderSn,
        contextHint: intent || undefined,
      },
      botIndex,
    )
  } catch (e) {
    messages.value[botIndex] = {
      role: 'bot',
      text: e instanceof Error ? e.message : '关联订单失败',
    }
  } finally {
    busy.value = false
    runId.value = ''
    await scrollBottom()
  }
}

async function pickOrder(order: OrderSummary) {
  if (busy.value || !requireLogin()) return
  busy.value = true
  messages.value.push({ role: 'sys', text: `已关联订单 ${order.orderSn}` })
  const botIndex = messages.value.push({ role: 'bot', text: '…' }) - 1
  try {
    await runChat(
      {
        message: '',
        conversationId: conversationId.value,
        contextOrderSn: order.orderSn,
        contextHint: order.status === 'WAIT_DELIVER' ? 'change_address' : undefined,
      },
      botIndex,
    )
  } catch (e) {
    messages.value[botIndex] = {
      role: 'bot',
      text: e instanceof Error ? e.message : '关联失败',
    }
  } finally {
    busy.value = false
    runId.value = ''
    await scrollBottom()
  }
}

function newSession() {
  abort?.abort()
  abort = null
  conversationId.value = crypto.randomUUID().replace(/-/g, '')
  localStorage.setItem(CID_KEY, conversationId.value)
  runId.value = ''
  streamHint.value = ''
  messages.value = [
    { role: 'sys', text: '已新开会话。上一会话的对话仍可由店主在后台取证查阅。' },
  ]
}

async function cancelCurrent() {
  if (!user.token || !runId.value) return
  await cancelChatRun(user.token, runId.value)
  abort?.abort()
}

async function send() {
  const text = input.value.trim()
  if (!text || busy.value) return
  if (!requireLogin()) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  busy.value = true
  const botIndex = messages.value.push({ role: 'bot', text: '…' }) - 1
  await scrollBottom()
  try {
    await runChat({ message: text, conversationId: conversationId.value }, botIndex)
  } catch {
    /* 错误已写入气泡 */
  } finally {
    busy.value = false
    runId.value = ''
    await scrollBottom()
  }
}

function quick(text: string) {
  input.value = text
  void send()
}

onMounted(async () => {
  if (!user.token) {
    messages.value.push({ role: 'sys', text: '登录后可带单查询与办理售后。' })
    return
  }
  await loadOrders()
  await bindEntryOrder()
  await scrollBottom()
})

onBeforeUnmount(() => {
  abort?.abort()
})
</script>

<template>
  <div class="container page">
    <div class="card chat">
      <header>
        <div>
          <h1>店内客服</h1>
          <p class="muted">会话 {{ conversationId.slice(0, 8) }}…</p>
        </div>
        <div class="actions">
          <button
            v-if="busy && runId"
            class="btn ghost"
            type="button"
            @click="cancelCurrent"
          >
            停止
          </button>
          <button class="btn ghost" type="button" :disabled="busy" @click="newSession">新开会话</button>
        </div>
      </header>
      <div v-if="recentOrders.length" class="orders">
        <span class="muted">点选订单带单：</span>
        <button
          v-for="o in recentOrders"
          :key="o.orderSn"
          type="button"
          class="chip"
          :disabled="busy"
          :title="o.orderSn"
          @click="pickOrder(o)"
        >
          {{ o.orderSn.slice(-6) }} · {{ orderStatusLabel(o.status) }}
        </button>
      </div>
      <p v-if="streamHint" class="hint muted">{{ streamHint }}</p>
      <div ref="logEl" class="log">
        <div v-for="(m, i) in messages" :key="i" class="bubble" :class="m.role">
          <div>{{ m.text }}</div>
          <details
            v-if="m.role === 'bot' && m.sources?.length"
            class="sources"
            :open="m.sourcesOpen"
            @toggle="m.sourcesOpen = ($event.target as HTMLDetailsElement).open"
          >
            <summary>知识来源（{{ m.sources.length }}）</summary>
            <ul>
              <li v-for="(s, si) in m.sources" :key="si">{{ s }}</li>
            </ul>
          </details>
        </div>
      </div>
      <form class="composer" @submit.prevent="send">
        <input v-model="input" :disabled="busy" placeholder="例如：七天无理由 / 查订单 / 改地址 / 转人工" />
        <button class="btn" type="submit" :disabled="busy">发送</button>
      </form>
      <div class="quick">
        <button type="button" :disabled="busy" @click="quick('七天无理由怎么退')">问退货政策</button>
        <button type="button" :disabled="busy" @click="quick('我的售后')">我的售后</button>
        <button type="button" :disabled="busy" @click="quick('转人工')">店主电话</button>
        <button type="button" :disabled="busy" @click="quick('帮我改收货地址')">改地址</button>
        <RouterLink to="/orders">订单列表</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { padding-top: 24px; max-width: 820px; }
.chat {
  display: grid;
  grid-template-rows: auto auto auto 1fr auto auto;
  min-height: min(70vh, 720px);
  overflow: hidden;
}
header {
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.actions { display: flex; gap: 8px; }
h1 { margin: 0; font-size: 1.2rem; }
.orders {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--line);
}
.hint {
  margin: 0;
  padding: 6px 16px;
  font-size: 0.85rem;
  border-bottom: 1px solid var(--line);
}
.chip {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.82rem;
  cursor: pointer;
}
.chip:disabled { opacity: 0.5; cursor: not-allowed; }
.log {
  padding: 16px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fafafa;
}
.bubble {
  max-width: 85%;
  padding: 10px 12px;
  border-radius: 12px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 0.95rem;
}
.bubble.user { align-self: flex-end; background: #111; color: #fff; }
.bubble.bot { align-self: flex-start; background: #fff; border: 1px solid var(--line); }
.bubble.sys { align-self: center; background: transparent; color: #888; font-size: 0.85rem; }
.sources {
  margin-top: 8px;
  font-size: 0.82rem;
  color: #666;
}
.sources summary { cursor: pointer; user-select: none; }
.sources ul { margin: 6px 0 0; padding-left: 1.1rem; }
.composer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}
.composer input {
  flex: 1;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
}
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 16px;
  align-items: center;
}
.quick button, .quick a {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.85rem;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}
.quick button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
