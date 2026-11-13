import axios from 'axios'

const agentBase = () => import.meta.env.VITE_AGENT_BASE || '/agent'

export interface AgentChatBody {
  reply?: string
  conversationId?: string
  intent?: string
  sources?: string[]
  confirm?: unknown
  debug?: Record<string, unknown>
}

export interface AgentChatPayload {
  message: string
  conversationId: string
  contextOrderSn?: string
  contextHint?: string
}

export type AgentStreamHandlers = {
  onStarted?: (runId: string, conversationId?: string) => void
  onProgress?: (text: string) => void
  onToken?: (text: string) => void
  onDone?: (body: AgentChatBody) => void
  onError?: (message: string) => void
  onCancelled?: () => void
}

function mapHttpError(status: number | undefined, raw: string): string {
  if (status === 401) return '请先登录后再使用客服'
  if (status === 403) return '没有权限执行该操作'
  if (status === 429) return raw || '客服繁忙，请稍后再试'
  if (status === 502 || status === 503 || status === 504) {
    return '客服服务暂时不可用，请确认 booth-agent 已启动'
  }
  if (!status) return '无法连接客服服务，请确认 booth-agent（默认 8091）已启动'
  return raw || `客服请求失败（${status}）`
}

export function agentErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    const data = error.response?.data as { message?: string; code?: number } | undefined
    return mapHttpError(status, data?.message || error.message)
  }
  if (error instanceof TypeError) {
    return '无法连接客服服务，请确认 booth-agent（默认 8091）已启动'
  }
  if (error instanceof Error) return error.message
  return '客服暂时不可用'
}

/** 校验 booth-agent 业务包装 { code, message, data } */
export function unwrapAgentResult<T>(payload: unknown): T {
  if (payload && typeof payload === 'object' && 'code' in payload) {
    const body = payload as { code: number; message?: string; data?: T }
    if (typeof body.code === 'number' && body.code !== 0) {
      throw new Error(body.message || '客服业务失败')
    }
    return (body.data ?? payload) as T
  }
  return payload as T
}

export async function postChatSync(
  token: string,
  payload: AgentChatPayload,
): Promise<AgentChatBody> {
  try {
    const { data } = await axios.post(`${agentBase()}/app/chat`, payload, {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 120_000,
    })
    return unwrapAgentResult<AgentChatBody>(data)
  } catch (error) {
    throw new Error(agentErrorMessage(error))
  }
}

/** SSE 流式对话；返回 AbortController 便于取消。 */
export function postChatStream(
  token: string,
  payload: AgentChatPayload,
  handlers: AgentStreamHandlers,
): AbortController {
  const controller = new AbortController()
  void (async () => {
    try {
      const res = await fetch(`${agentBase()}/app/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
        body: JSON.stringify(payload),
        signal: controller.signal,
      })
      if (!res.ok) {
        let message = mapHttpError(res.status, '')
        try {
          const json = (await res.json()) as { message?: string }
          if (json.message) message = mapHttpError(res.status, json.message)
        } catch {
          /* ignore */
        }
        handlers.onError?.(message)
        return
      }
      if (!res.body) {
        handlers.onError?.('浏览器不支持流式响应')
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const chunks = buffer.split('\n\n')
        buffer = chunks.pop() || ''
        for (const chunk of chunks) {
          const lines = chunk.split('\n')
          let eventName = 'message'
          let data = ''
          for (const line of lines) {
            if (line.startsWith('event:')) eventName = line.slice(6).trim()
            if (line.startsWith('data:')) data += line.slice(5).trim()
          }
          if (!data) continue
          let payloadJson: Record<string, unknown> = {}
          try {
            payloadJson = JSON.parse(data) as Record<string, unknown>
          } catch {
            payloadJson = { text: data }
          }
          if (eventName === 'started') {
            handlers.onStarted?.(
              String(payloadJson.runId || ''),
              payloadJson.conversationId ? String(payloadJson.conversationId) : undefined,
            )
          } else if (eventName === 'progress') {
            handlers.onProgress?.(String(payloadJson.text || '处理中'))
          } else if (eventName === 'token') {
            handlers.onToken?.(String(payloadJson.text || ''))
          } else if (eventName === 'cancelled') {
            handlers.onCancelled?.()
          } else if (eventName === 'done') {
            handlers.onDone?.(payloadJson as AgentChatBody)
          } else if (eventName === 'error') {
            handlers.onError?.(String(payloadJson.message || '客服出错了'))
          }
        }
      }
    } catch (error) {
      if (controller.signal.aborted) {
        handlers.onCancelled?.()
        return
      }
      handlers.onError?.(agentErrorMessage(error))
    }
  })()
  return controller
}

export async function cancelChatRun(token: string, runId: string): Promise<void> {
  if (!runId) return
  try {
    await axios.post(
      `${agentBase()}/app/chat/cancel`,
      { runId },
      { headers: { Authorization: `Bearer ${token}` } },
    )
  } catch {
    /* ignore */
  }
}
