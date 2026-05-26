import type { RendererProps } from '../ComponentRegistry'

export const ColumnRenderer: React.FC<RendererProps> = ({ node, renderChildren }) => {
  const gap = (node.props['spacing'] as number) ?? 0
  const padding = (node.props['padding'] as number) ?? 0
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap, padding }}>
      {renderChildren()}
    </div>
  )
}

export const RowRenderer: React.FC<RendererProps> = ({ node, renderChildren }) => {
  const gap = (node.props['spacing'] as number) ?? 0
  const align = (node.props['verticalAlignment'] as string) ?? 'top'
  const alignItems = align === 'center' ? 'center' : align === 'bottom' ? 'flex-end' : 'flex-start'
  return (
    <div style={{ display: 'flex', flexDirection: 'row', gap, alignItems }}>
      {renderChildren()}
    </div>
  )
}

export const TextRenderer: React.FC<RendererProps> = ({ node, stateStore }) => {
  const text = resolveValue(node, 'text', stateStore) as string ?? ''
  const fontSize = (node.props['fontSize'] as number) ?? 13
  const bold = (node.props['bold'] as boolean) ?? false
  const color = node.props['color'] as string | undefined
  return (
    <span style={{ fontSize, fontWeight: bold ? 600 : 400, color: color ?? 'var(--mdt-fg)' }}>
      {text}
    </span>
  )
}

export const ButtonRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const label = resolveValue(node, 'label', stateStore) as string ?? 'Button'
  const disabled = (node.props['disabled'] as boolean) ?? false
  return (
    <button
      disabled={disabled}
      onClick={() => onEvent(node.id, 'click')}
      style={{
        padding: '4px 12px', fontSize: 12, cursor: disabled ? 'default' : 'pointer',
        background: 'var(--mdt-accent)', color: '#fff', border: 'none', borderRadius: 4,
        opacity: disabled ? 0.5 : 1,
      }}
    >
      {label}
    </button>
  )
}

function resolveValue(node: { props: Record<string, unknown>; bindings: { prop: string; path: string }[] }, prop: string, stateStore: { getValue(path: string): unknown }): unknown {
  const binding = node.bindings.find(b => b.prop === prop)
  if (binding) {
    const val = stateStore.getValue(binding.path)
    if (val !== undefined) return val
  }
  return node.props[prop]
}
