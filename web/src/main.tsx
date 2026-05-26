import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { initBridge, isInsideJcef } from './bridge'
import { App } from './App'
import './viewers/registerAll'
import './styles/global.css'

initBridge()

if (!isInsideJcef()) {
  import('./dev/mockBridge').then((m) => m.installMockBridge())
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
