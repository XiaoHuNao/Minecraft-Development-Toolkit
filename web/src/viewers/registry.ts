import type { FC } from 'react'

export interface ViewerProps {
  json: object
  rawText: string
  fileName: string
}

const viewers: Record<string, FC<ViewerProps>> = {}

export function registerViewer(type: string, component: FC<ViewerProps>) {
  viewers[type] = component
}

export function getViewer(type: string): FC<ViewerProps> | undefined {
  return viewers[type]
}

export function registeredTypes(): string[] {
  return Object.keys(viewers)
}
