import type { BridgeMessage, PluginCallback, PluginCallbackType } from './types'

type MessageHandler = (message: BridgeMessage) => void

const listeners: MessageHandler[] = []

export function onBridgeMessage(handler: MessageHandler): () => void {
  listeners.push(handler)
  return () => {
    const idx = listeners.indexOf(handler)
    if (idx >= 0) listeners.splice(idx, 1)
  }
}

function handleIncoming(raw: string) {
  try {
    const message: BridgeMessage = JSON.parse(raw)
    listeners.forEach((fn) => fn(message))
  } catch (e) {
    console.error('[MDT Bridge] Failed to parse message:', e)
  }
}

declare global {
  interface Window {
    __MDT_BRIDGE__?: { receive: (raw: string) => void }
    cefQuery?: (params: {
      request: string
      persistent: boolean
      onSuccess: (response: string) => void
      onFailure: (code: number, msg: string) => void
    }) => void
  }
}

export function initBridge() {
  window.__MDT_BRIDGE__ = { receive: handleIncoming }
  ;(window.__MDT_BRIDGE__ as any).__ready = true
}

export function sendToPlugin(type: PluginCallbackType, payload?: unknown) {
  const msg: PluginCallback = { type, payload, timestamp: Date.now() }
  const json = JSON.stringify(msg)
  if (window.cefQuery) {
    window.cefQuery({
      request: json,
      persistent: false,
      onSuccess: () => {},
      onFailure: () => {},
    })
  } else {
    console.debug('[MDT Bridge] No cefQuery, message dropped:', type)
  }
}

export function isInsideJcef(): boolean {
  return typeof window.cefQuery === 'function'
}
