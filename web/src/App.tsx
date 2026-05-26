import { useState } from 'react'
import { useBridgeMessage } from './bridge'
import type { ModeSetPayload } from './bridge'
import { ThemeProvider } from './theme/ThemeProvider'
import { ViewerApp } from './viewers/ViewerApp'
import { ToolWindowApp } from './toolwindow/ToolWindowApp'

type AppMode = 'viewer' | 'toolwindow'

function getInitialMode(): AppMode {
  const params = new URLSearchParams(location.search)
  return (params.get('mode') as AppMode) ?? 'viewer'
}

export function App() {
  const [mode, setMode] = useState<AppMode>(getInitialMode)

  useBridgeMessage<ModeSetPayload>('MODE_SET', (payload) => {
    setMode(payload.mode)
  })

  return (
    <ThemeProvider>
      {mode === 'viewer' ? <ViewerApp /> : <ToolWindowApp />}
    </ThemeProvider>
  )
}
