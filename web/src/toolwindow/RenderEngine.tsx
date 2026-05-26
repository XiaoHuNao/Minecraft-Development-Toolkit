import type { ComponentNode } from '../types/protocol'
import { ComponentRegistry } from './ComponentRegistry'
import { StateStore } from './StateStore'

interface RenderEngineProps {
  components: ComponentNode[]
  stateStore: StateStore
  onEvent: (componentId: string, eventType: string, value?: unknown) => void
  registry: ComponentRegistry
}

export function RenderEngine({ components, stateStore, onEvent, registry }: RenderEngineProps) {
  const componentMap = new Map(components.map(c => [c.id, c]))
  const childIds = new Set(components.flatMap(c => c.children))
  const rootNodes = components.filter(c => !childIds.has(c.id))

  return (
    <>
      {rootNodes.map(node => (
        <RenderNode
          key={node.id}
          node={node}
          componentMap={componentMap}
          stateStore={stateStore}
          onEvent={onEvent}
          registry={registry}
        />
      ))}
    </>
  )
}

interface RenderNodeProps {
  node: ComponentNode
  componentMap: Map<string, ComponentNode>
  stateStore: StateStore
  onEvent: (componentId: string, eventType: string, value?: unknown) => void
  registry: ComponentRegistry
}

function RenderNode({ node, componentMap, stateStore, onEvent, registry }: RenderNodeProps) {
  const Renderer = registry.get(node.type)

  const renderChildren = () => (
    <>
      {node.children.map(childId => {
        const child = componentMap.get(childId)
        if (!child) return null
        return (
          <RenderNode
            key={child.id}
            node={child}
            componentMap={componentMap}
            stateStore={stateStore}
            onEvent={onEvent}
            registry={registry}
          />
        )
      })}
    </>
  )

  if (!Renderer) {
    return <span style={{ color: 'gray', fontSize: 11 }}>Unknown: {node.type}</span>
  }

  return <Renderer node={node} stateStore={stateStore} onEvent={onEvent} renderChildren={renderChildren} />
}
