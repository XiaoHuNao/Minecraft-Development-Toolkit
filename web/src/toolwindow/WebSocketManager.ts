import type { Message } from '../types/protocol'
import { createMessage } from '../types/protocol'

export type ConnectionState = 'disconnected' | 'connecting' | 'handshaking' | 'connected' | 'reconnecting'

interface WebSocketManagerConfig {
  host: string
  port: number
  token: string
  reconnect?: boolean
  supportedComponents?: string[]
}

type StateChangeHandler = (state: ConnectionState) => void
type MessageHandler = (message: Message) => void

const DEFAULT_COMPONENTS = [
  'column', 'row', 'text', 'button', 'textField', 'checkbox',
  'dropdown', 'lazyColumn', 'progressBar', 'divider', 'spacer',
  'card', 'iconButton', 'textArea', 'slider', 'tabStrip',
  'icon', 'banner', 'markdown', 'table', 'tree', 'toolbarDecorator',
]

export class WebSocketManager {
  private ws: WebSocket | null = null
  private state: ConnectionState = 'disconnected'
  private config: Required<WebSocketManagerConfig>
  private reconnectAttempt = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private handshakeTimer: ReturnType<typeof setTimeout> | null = null
  private offlineQueue: string[] = []
  private userDisconnect = false

  private stateHandlers: StateChangeHandler[] = []
  private messageHandlers: MessageHandler[] = []

  constructor(config: WebSocketManagerConfig) {
    this.config = {
      reconnect: true,
      supportedComponents: DEFAULT_COMPONENTS,
      ...config,
    }
  }

  onStateChange(handler: StateChangeHandler): () => void {
    this.stateHandlers.push(handler)
    return () => { this.stateHandlers = this.stateHandlers.filter(h => h !== handler) }
  }

  onMessage(handler: MessageHandler): () => void {
    this.messageHandlers.push(handler)
    return () => { this.messageHandlers = this.messageHandlers.filter(h => h !== handler) }
  }

  getState(): ConnectionState { return this.state }

  connect() {
    if (this.state === 'connected' || this.state === 'handshaking') return
    if (this.state !== 'disconnected' && this.state !== 'reconnecting') return

    this.userDisconnect = false
    this.setState('connecting')
    const url = `ws://${this.config.host}:${this.config.port}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.setState('handshaking')
      this.reconnectAttempt = 0
      this.sendReady()
      this.startHandshakeTimeout()
      this.startHeartbeat()
      this.flushOfflineQueue()
    }

    this.ws.onmessage = (event) => {
      try {
        const message: Message = JSON.parse(event.data as string)
        this.handleMessage(message)
      } catch (e) {
        console.error('[MDT WS] Failed to parse message', e)
      }
    }

    this.ws.onclose = () => { this.handleDisconnect() }
    this.ws.onerror = () => { this.handleDisconnect() }
  }

  disconnect() {
    this.userDisconnect = true
    this.setState('disconnected')
    this.stopHeartbeat()
    this.cancelHandshakeTimeout()
    this.clearReconnectTimer()
    this.ws?.close(1000, 'Client disconnect')
    this.ws = null
  }

  send(message: Message) {
    const json = JSON.stringify(message)
    if (this.ws && (this.state === 'connected' || this.state === 'handshaking')) {
      this.ws.send(json)
    } else {
      this.offlineQueue.push(json)
    }
  }

  private handleMessage(message: Message) {
    switch (message.type) {
      case 'HELLO':
        this.cancelHandshakeTimeout()
        this.setState('connected')
        this.messageHandlers.forEach(h => h(message))
        break
      case 'PING': {
        const ts = (message.payload['timestamp'] as number) ?? 0
        this.send(createMessage('PONG', { timestamp: ts }))
        break
      }
      case 'ERROR': {
        const code = message.payload['code'] as string | undefined
        if (code === 'AUTH_FAILED') {
          this.disconnect()
        } else {
          this.messageHandlers.forEach(h => h(message))
        }
        break
      }
      default:
        this.messageHandlers.forEach(h => h(message))
    }
  }

  private sendReady() {
    const msg = createMessage('READY', {
      protocolVersion: '1.0.0',
      supportedComponents: this.config.supportedComponents,
      authToken: this.config.token,
    })
    this.send(msg)
  }

  private handleDisconnect() {
    if (this.userDisconnect) return
    this.stopHeartbeat()
    this.cancelHandshakeTimeout()
    this.ws = null
    if (this.config.reconnect && this.state !== 'disconnected') {
      this.setState('reconnecting')
      this.scheduleReconnect()
    } else {
      this.setState('disconnected')
    }
  }

  private scheduleReconnect() {
    this.clearReconnectTimer()
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempt), 30000)
    this.reconnectAttempt++
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send(createMessage('PING', { timestamp: Date.now() }))
    }, 30000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) { clearInterval(this.heartbeatTimer); this.heartbeatTimer = null }
  }

  private startHandshakeTimeout() {
    this.cancelHandshakeTimeout()
    this.handshakeTimer = setTimeout(() => {
      if (this.state === 'handshaking') {
        this.ws?.close(4004, 'Handshake timeout')
        this.handleDisconnect()
      }
    }, 10000)
  }

  private cancelHandshakeTimeout() {
    if (this.handshakeTimer) { clearTimeout(this.handshakeTimer); this.handshakeTimer = null }
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) { clearTimeout(this.reconnectTimer); this.reconnectTimer = null }
  }

  private flushOfflineQueue() {
    if (!this.ws) return
    this.offlineQueue.forEach(json => this.ws!.send(json))
    this.offlineQueue = []
  }

  private setState(newState: ConnectionState) {
    if (this.state === newState) return
    this.state = newState
    this.stateHandlers.forEach(h => h(newState))
  }
}
