import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { initBridge } from './bridge'
import { App } from './App'
import './viewers/registerAll'
import './styles/global.css'

initBridge()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
