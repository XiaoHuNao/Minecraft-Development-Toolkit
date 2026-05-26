import type { ComponentNode } from '../types/protocol'
import { StateStore } from './StateStore'

export interface RendererProps {
  node: ComponentNode
  stateStore: StateStore
  onEvent: (componentId: string, eventType: string, value?: unknown) => void
  renderChildren: () => React.ReactNode
}

type ComponentRenderer = React.FC<RendererProps>

export class ComponentRegistry {
  private renderers = new Map<string, ComponentRenderer>()

  register(type: string, renderer: ComponentRenderer) {
    this.renderers.set(type, renderer)
  }

  get(type: string): ComponentRenderer | undefined {
    return this.renderers.get(type)
  }

  has(type: string): boolean {
    return this.renderers.has(type)
  }
}
