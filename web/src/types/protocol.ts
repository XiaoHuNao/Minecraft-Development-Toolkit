export type MessageType =
  | 'READY' | 'HELLO' | 'UI_RENDER'
  | 'STATE_SNAPSHOT' | 'STATE_DELTA'
  | 'TAB_OPEN' | 'TAB_CLOSE' | 'TAB_UPDATE'
  | 'NOTIFICATION' | 'CONSOLE_APPEND'
  | 'COMMAND_INPUT' | 'COMMAND_TREE'
  | 'COMMAND_SUGGEST_REQUEST' | 'COMMAND_SUGGEST_RESPONSE'
  | 'EVENT_FIRE' | 'EVENT_ACK'
  | 'PING' | 'PONG' | 'ERROR'

export interface Message {
  type: MessageType
  tabId?: string
  requestId?: string
  payload: Record<string, unknown>
  timestamp: number
}

export interface ComponentNode {
  id: string
  type: string
  props: Record<string, unknown>
  children: string[]
  bindings: Binding[]
  events: string[]
}

export interface Binding {
  prop: string
  path: string
  direction: 'ONE_WAY' | 'TWO_WAY'
}

export interface TabDescriptor {
  id: string
  title: string
  icon: string
  closeable: boolean
  order: number
}

export interface StatePatch {
  baseVersion: number
  patch: PatchOperation[]
}

export interface PatchOperation {
  op: 'add' | 'replace' | 'remove'
  path: string
  value?: unknown
}

export function createMessage(type: MessageType, payload: Record<string, unknown> = {}, tabId?: string, requestId?: string): Message {
  return { type, payload, tabId, requestId, timestamp: Date.now() }
}
