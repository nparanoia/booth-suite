/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE: string
  readonly VITE_AGENT_BASE: string
  readonly VITE_BRAND: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
