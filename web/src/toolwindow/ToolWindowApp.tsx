import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useBridgeMessage } from '../bridge'
import type { ConnectionParamsPayload } from '../bridge'
import type { ComponentNode, TabDescriptor } from '../types/protocol'
import { createMessage } from '../types/protocol'
import { WebSocketManager, type ConnectionState } from './WebSocketManager'
import { MessageDispatcher } from './MessageDispatcher'
import { StateStore } from './StateStore'
import { RenderEngine } from './RenderEngine'
import { createDefaultRegistry } from './components'
import { ConsoleView } from './ConsoleView'

interface ConsoleEntry { level: 'INFO' | 'WARN' | 'ERROR' | 'COMMAND'; message: string }

interface TabContent {
  components: ComponentNode[]
  stateStore: StateStore
}

export function ToolWindowApp() {
  const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected')
  const [serverName, setServerName] = useState<string>('')
  const [tabs, setTabs] = useState<TabDescriptor[]>([])
  const [selectedTab, setSelectedTab] = useState<string>('console')
  const [consoleEntries, setConsoleEntries] = useState<ConsoleEntry[]>([])
  const [suggestions, setSuggestions] = useState<string[]>([])
  const tabContents = useRef<Map<string, TabContent>>(new Map())
  const [, setRenderKey] = useState(0)

  const wsRef = useRef<WebSocketManager | null>(null)
  const dispatcherRef = useRef(new MessageDispatcher())
  const registry = useMemo(() => createDefaultRegistry(), [])

  const forceRender = useCallback(() => setRenderKey(k => k + 1), [])

  useEffect(() => {
    const dispatcher = dispatcherRef.current

    dispatcher.onHello((msg) => {
      const tabsArr = msg.payload['tabs'] as TabDescriptor[] | undefined
      if (tabsArr) {
        setTabs(tabsArr.filter(t => t.id !== 'console').sort((a, b) => a.order - b.order))
      }
      setServerName((msg.payload['serverName'] as string) ?? '服务器')
    })

    dispatcher.onUiRender((tabId, msg) => {
      const components = (msg.payload['components'] as ComponentNode[]) ?? []
      const stateObj = msg.payload['state'] as Record<string, unknown> | undefined
      const baseVersion = (msg.payload['baseVersion'] as number) ?? 1

      let content = tabContents.current.get(tabId)
      if (!content) {
        content = { components: [], stateStore: new StateStore() }
        tabContents.current.set(tabId, content)
      }
      content.components = components
      if (stateObj) content.stateStore.applySnapshot(stateObj, baseVersion)
      forceRender()
    })

    dispatcher.onStateUpdate((tabId, msg) => {
      const content = tabContents.current.get(tabId)
      if (!content) return
      if (msg.type === 'STATE_SNAPSHOT') {
        const state = msg.payload['state'] as Record<string, unknown>
        const version = msg.payload['baseVersion'] as number
        if (state && version != null) content.stateStore.applySnapshot(state, version)
      } else if (msg.type === 'STATE_DELTA') {
        const patch = { baseVersion: msg.payload['baseVersion'] as number, patch: msg.payload['patch'] as any[] }
        content.stateStore.applyPatch(patch)
      }
      forceRender()
    })

    dispatcher.onTabChange((msg) => {
      if (msg.type === 'TAB_OPEN') {
        const desc = msg.payload as unknown as TabDescriptor
        if (desc.id === 'console') return
        setTabs(prev => {
          if (prev.some(t => t.id === desc.id)) return prev
          return [...prev, desc].sort((a, b) => a.order - b.order)
        })
      } else if (msg.type === 'TAB_CLOSE') {
        const tabId = msg.tabId
        if (!tabId) return
        setTabs(prev => prev.filter(t => t.id !== tabId))
        tabContents.current.delete(tabId)
        setSelectedTab(prev => prev === tabId ? 'console' : prev)
        forceRender()
      } else if (msg.type === 'TAB_UPDATE') {
        const desc = msg.payload as unknown as TabDescriptor
        setTabs(prev => prev.map(t => t.id === desc.id ? desc : t))
      }
    })

    dispatcher.onConsoleAppend((_tabId, msg) => {
      const lines = msg.payload['lines'] as Array<{ level?: string; message?: string }> | undefined
      if (!lines) return
      const entries: ConsoleEntry[] = lines.map(line => ({
        level: (line.level?.toUpperCase() as ConsoleEntry['level']) ?? 'INFO',
        message: line.message ?? '',
      }))
      setConsoleEntries(prev => [...prev, ...entries])
    })

    dispatcher.onCommandSuggestResponse((msg) => {
      const items = (msg.payload['suggestions'] as string[]) ?? []
      setSuggestions(items)
    })
  }, [forceRender])

  useBridgeMessage<ConnectionParamsPayload>('CONNECTION_PARAMS', ({ host, port, token }) => {
    if (wsRef.current) wsRef.current.disconnect()
    const ws = new WebSocketManager({ host, port, token })
    wsRef.current = ws
    ws.onStateChange(setConnectionState)
    ws.onMessage((msg) => dispatcherRef.current.dispatch(msg))
    ws.connect()
  })

  const handleCommand = (command: string) => {
    if (!wsRef.current) return
    wsRef.current.send(createMessage('COMMAND_INPUT', { command }))
    setConsoleEntries(prev => [...prev, { level: 'COMMAND', message: command }])
  }

  const handleTabComplete = (input: string, cursor: number) => {
    if (!wsRef.current) return
    wsRef.current.send(createMessage('COMMAND_SUGGEST_REQUEST', { input, cursor }))
  }

  const handleSuggestionSelect = (_suggestion: string) => {
    setSuggestions([])
  }

  const handleEvent = (componentId: string, eventType: string, value?: unknown) => {
    if (!wsRef.current || !selectedTab) return
    wsRef.current.send(createMessage('EVENT_FIRE', { componentId, eventType, value }, selectedTab))
  }

  const handleDisconnect = () => {
    wsRef.current?.disconnect()
    wsRef.current = null
    setConnectionState('disconnected')
    setServerName('')
    setTabs([])
    setSelectedTab('console')
    tabContents.current.clear()
    setConsoleEntries([])
    forceRender()
  }

  if (connectionState === 'disconnected') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
        <p style={{ color: 'var(--mdt-muted)', fontSize: 13 }}>等待连接参数...</p>
      </div>
    )
  }

  if (connectionState === 'connecting' || connectionState === 'handshaking') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
        <p style={{ color: 'var(--mdt-muted)', fontSize: 13 }}>正在连接服务器...</p>
      </div>
    )
  }

  if (connectionState === 'reconnecting') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
        <p style={{ color: '#ff9800', fontSize: 13 }}>连接断开，正在重连...</p>
      </div>
    )
  }

  const currentContent = selectedTab !== 'console' ? tabContents.current.get(selectedTab) : null

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar serverName={serverName} onDisconnect={handleDisconnect} />
      {tabs.length > 0 && (
        <div style={{ display: 'flex', borderBottom: '1px solid var(--mdt-border)' }}>
          <TabButton label="控制台" active={selectedTab === 'console'} onClick={() => setSelectedTab('console')} />
          {tabs.map(tab => (
            <TabButton key={tab.id} label={tab.title} active={selectedTab === tab.id} onClick={() => setSelectedTab(tab.id)} />
          ))}
        </div>
      )}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {selectedTab === 'console' ? (
          <ConsoleView
            entries={consoleEntries}
            onCommand={handleCommand}
            onTabComplete={handleTabComplete}
            suggestions={suggestions}
            onSuggestionSelect={handleSuggestionSelect}
          />
        ) : currentContent ? (
          <div style={{ height: '100%', overflow: 'auto', padding: 8 }}>
            <RenderEngine
              components={currentContent.components}
              stateStore={currentContent.stateStore}
              onEvent={handleEvent}
              registry={registry}
            />
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <p style={{ color: 'var(--mdt-muted)', fontSize: 12 }}>加载中...</p>
          </div>
        )}
      </div>
    </div>
  )
}

function Toolbar({ serverName, onDisconnect }: { serverName: string; onDisconnect: () => void }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '4px 8px', borderBottom: '1px solid var(--mdt-border)' }}>
      <button onClick={onDisconnect} style={{ fontSize: 12, padding: '2px 8px', cursor: 'pointer', background: 'var(--mdt-card-bg)', border: '1px solid var(--mdt-border)', borderRadius: 4, color: 'var(--mdt-fg)' }}>
        断开连接
      </button>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#4caf50' }} />
        <span style={{ fontSize: 11, color: 'var(--mdt-muted)' }}>{serverName}</span>
      </div>
    </div>
  )
}

function TabButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: '6px 12px', fontSize: 12, cursor: 'pointer',
        background: 'none', border: 'none', color: 'var(--mdt-fg)',
        borderBottom: active ? '2px solid var(--mdt-accent)' : '2px solid transparent',
        fontWeight: active ? 600 : 400,
      }}
    >
      {label}
    </button>
  )
}
