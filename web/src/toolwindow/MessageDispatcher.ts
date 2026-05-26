import type { Message } from '../types/protocol'

type TabMessageHandler = (tabId: string, message: Message) => void
type GlobalMessageHandler = (message: Message) => void

export class MessageDispatcher {
  private helloHandlers: GlobalMessageHandler[] = []
  private uiRenderHandlers: TabMessageHandler[] = []
  private stateHandlers: TabMessageHandler[] = []
  private tabHandlers: GlobalMessageHandler[] = []
  private notificationHandlers: GlobalMessageHandler[] = []
  private consoleHandlers: TabMessageHandler[] = []
  private commandSuggestHandlers: GlobalMessageHandler[] = []

  onHello(handler: GlobalMessageHandler) { this.helloHandlers.push(handler) }
  onUiRender(handler: TabMessageHandler) { this.uiRenderHandlers.push(handler) }
  onStateUpdate(handler: TabMessageHandler) { this.stateHandlers.push(handler) }
  onTabChange(handler: GlobalMessageHandler) { this.tabHandlers.push(handler) }
  onNotification(handler: GlobalMessageHandler) { this.notificationHandlers.push(handler) }
  onConsoleAppend(handler: TabMessageHandler) { this.consoleHandlers.push(handler) }
  onCommandSuggestResponse(handler: GlobalMessageHandler) { this.commandSuggestHandlers.push(handler) }

  dispatch(message: Message) {
    const tabId = message.tabId

    switch (message.type) {
      case 'HELLO':
        this.helloHandlers.forEach(h => h(message))
        break
      case 'UI_RENDER':
        if (tabId) this.uiRenderHandlers.forEach(h => h(tabId, message))
        break
      case 'STATE_SNAPSHOT':
      case 'STATE_DELTA':
        if (tabId) this.stateHandlers.forEach(h => h(tabId, message))
        break
      case 'TAB_OPEN':
      case 'TAB_CLOSE':
      case 'TAB_UPDATE':
        this.tabHandlers.forEach(h => h(message))
        break
      case 'NOTIFICATION':
        this.notificationHandlers.forEach(h => h(message))
        break
      case 'CONSOLE_APPEND':
        if (tabId) this.consoleHandlers.forEach(h => h(tabId, message))
        break
      case 'COMMAND_SUGGEST_RESPONSE':
        this.commandSuggestHandlers.forEach(h => h(message))
        break
      case 'EVENT_ACK':
        break
      case 'ERROR':
        console.error('[MDT] Server error:', message.payload)
        break
      default:
        console.warn('[MDT] Unhandled message type:', message.type)
    }
  }
}
