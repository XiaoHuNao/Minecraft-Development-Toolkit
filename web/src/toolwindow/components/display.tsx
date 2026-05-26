import type { RendererProps } from '../ComponentRegistry'

export const DividerRenderer: React.FC<RendererProps> = ({ node }) => {
  const orientation = (node.props['orientation'] as string) ?? 'horizontal'
  if (orientation === 'vertical') {
    return <div style={{ width: 1, alignSelf: 'stretch', background: 'var(--mdt-border)' }} />
  }
  return <hr style={{ border: 'none', borderTop: '1px solid var(--mdt-border)', margin: '4px 0' }} />
}

export const SpacerRenderer: React.FC<RendererProps> = ({ node }) => {
  const height = (node.props['height'] as number) ?? 8
  const width = (node.props['width'] as number) ?? undefined
  return <div style={{ height, width }} />
}

export const CardRenderer: React.FC<RendererProps> = ({ node, renderChildren }) => {
  const padding = (node.props['padding'] as number) ?? 12
  return (
    <div style={{
      background: 'var(--mdt-card-bg)', border: '1px solid var(--mdt-border)',
      borderRadius: 8, padding,
    }}>
      {renderChildren()}
    </div>
  )
}

export const ProgressBarRenderer: React.FC<RendererProps> = ({ node, stateStore }) => {
  const binding = node.bindings.find(b => b.prop === 'value')
  const value = binding ? (stateStore.getValue(binding.path) as number ?? 0) : (node.props['value'] as number ?? 0)
  const max = (node.props['max'] as number) ?? 100
  const pct = Math.min(100, (value / max) * 100)
  return (
    <div style={{ height: 6, background: 'var(--mdt-border)', borderRadius: 3, overflow: 'hidden' }}>
      <div style={{ height: '100%', width: `${pct}%`, background: 'var(--mdt-accent)', transition: 'width 0.2s' }} />
    </div>
  )
}

export const LazyColumnRenderer: React.FC<RendererProps> = ({ renderChildren }) => (
  <div style={{ display: 'flex', flexDirection: 'column', overflow: 'auto', maxHeight: '100%' }}>
    {renderChildren()}
  </div>
)

export const IconRenderer: React.FC<RendererProps> = ({ node }) => {
  const name = (node.props['name'] as string) ?? ''
  const size = (node.props['size'] as number) ?? 16
  return <span style={{ fontSize: size, color: 'var(--mdt-muted)' }}>{name}</span>
}

export const IconButtonRenderer: React.FC<RendererProps> = ({ node, onEvent }) => {
  const icon = (node.props['icon'] as string) ?? '⚙'
  const tooltip = (node.props['tooltip'] as string) ?? ''
  return (
    <button
      title={tooltip}
      onClick={() => onEvent(node.id, 'click')}
      style={{
        background: 'none', border: 'none', cursor: 'pointer',
        padding: 4, borderRadius: 4, fontSize: 14, color: 'var(--mdt-fg)',
      }}
    >
      {icon}
    </button>
  )
}

export const BannerRenderer: React.FC<RendererProps> = ({ node }) => {
  const message = (node.props['message'] as string) ?? ''
  const severity = (node.props['severity'] as string) ?? 'info'
  const colors: Record<string, string> = { error: 'var(--mdt-error)', warning: '#ff9800', info: 'var(--mdt-accent)', success: '#4caf50' }
  return (
    <div style={{
      padding: '8px 12px', borderRadius: 4, fontSize: 12,
      borderLeft: `3px solid ${colors[severity] ?? colors['info']}`,
      background: 'var(--mdt-card-bg)', color: 'var(--mdt-fg)',
    }}>
      {message}
    </div>
  )
}
