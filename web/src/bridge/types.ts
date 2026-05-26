export type BridgeMessageType =
  | 'DOCUMENT_UPDATE'
  | 'THEME_UPDATE'
  | 'CONNECTION_PARAMS'
  | 'MODE_SET'

export interface BridgeMessage<T = unknown> {
  type: BridgeMessageType
  payload: T
  timestamp: number
}

export interface DocumentUpdatePayload {
  text: string
  filePath: string
  resourceType: string | null
}

export interface ThemeUpdatePayload {
  bg: string
  fg: string
  border: string
  accent: string
  cardBg: string
  cardHover: string
  muted: string
  error: string
}

export interface ConnectionParamsPayload {
  host: string
  port: number
  token: string
}

export interface ModeSetPayload {
  mode: 'viewer' | 'toolwindow'
}

export type PluginCallbackType = 'VIEWER_READY' | 'TOGGLE_RAW_JSON'

export interface PluginCallback {
  type: PluginCallbackType
  payload?: unknown
  timestamp: number
}
