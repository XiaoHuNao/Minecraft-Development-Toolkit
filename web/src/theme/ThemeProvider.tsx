import { useEffect } from 'react'
import { useBridgeMessage } from '../bridge'
import type { ThemeUpdatePayload } from '../bridge'

function isLightColor(hex: string): boolean {
  const c = hex.replace('#', '')
  const r = parseInt(c.substring(0, 2), 16)
  const g = parseInt(c.substring(2, 4), 16)
  const b = parseInt(c.substring(4, 6), 16)
  return (r * 299 + g * 587 + b * 114) / 1000 > 128
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  useBridgeMessage<ThemeUpdatePayload>('THEME_UPDATE', (colors) => {
    const root = document.documentElement
    root.style.setProperty('--mdt-bg', colors.bg)
    root.style.setProperty('--mdt-fg', colors.fg)
    root.style.setProperty('--mdt-border', colors.border)
    root.style.setProperty('--mdt-accent', colors.accent)
    root.style.setProperty('--mdt-card-bg', colors.cardBg)
    root.style.setProperty('--mdt-card-hover', colors.cardHover)
    root.style.setProperty('--mdt-muted', colors.muted)
    root.style.setProperty('--mdt-error', colors.error)
    root.dataset.theme = isLightColor(colors.bg) ? 'light' : 'dark'
  })

  useEffect(() => {
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () => {
      document.documentElement.dataset.theme = mql.matches ? 'dark' : 'light'
    }
    apply()
    mql.addEventListener('change', apply)
    return () => mql.removeEventListener('change', apply)
  }, [])

  return <>{children}</>
}
