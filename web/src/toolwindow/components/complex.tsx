import { useState } from 'react'
import type { RendererProps } from '../ComponentRegistry'

export const TabStripRenderer: React.FC<RendererProps> = ({ node, onEvent }) => {
  const tabs = (node.props['tabs'] as string[]) ?? []
  const [selected, setSelected] = useState((node.props['selected'] as number) ?? 0)

  return (
    <div style={{ display: 'flex', borderBottom: '1px solid var(--mdt-border)', gap: 0 }}>
      {tabs.map((tab, i) => (
        <button
          key={i}
          onClick={() => { setSelected(i); onEvent(node.id, 'select', i) }}
          style={{
            padding: '6px 12px', fontSize: 12, cursor: 'pointer',
            background: 'none', border: 'none', color: 'var(--mdt-fg)',
            borderBottom: i === selected ? '2px solid var(--mdt-accent)' : '2px solid transparent',
            fontWeight: i === selected ? 600 : 400,
          }}
        >
          {tab}
        </button>
      ))}
    </div>
  )
}

export const MarkdownRenderer: React.FC<RendererProps> = ({ node, stateStore }) => {
  const binding = node.bindings.find(b => b.prop === 'content')
  const content = binding ? (stateStore.getValue(binding.path) as string ?? '') : (node.props['content'] as string ?? '')
  return <pre style={{ fontSize: 12, whiteSpace: 'pre-wrap', color: 'var(--mdt-fg)' }}>{content}</pre>
}

export const TableRenderer: React.FC<RendererProps> = ({ node }) => {
  const headers = (node.props['headers'] as string[]) ?? []
  const rows = (node.props['rows'] as string[][]) ?? []
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
      {headers.length > 0 && (
        <thead>
          <tr>
            {headers.map((h, i) => (
              <th key={i} style={{ textAlign: 'left', padding: '4px 8px', borderBottom: '1px solid var(--mdt-border)', fontWeight: 600 }}>{h}</th>
            ))}
          </tr>
        </thead>
      )}
      <tbody>
        {rows.map((row, ri) => (
          <tr key={ri}>
            {row.map((cell, ci) => (
              <td key={ci} style={{ padding: '4px 8px', borderBottom: '1px solid var(--mdt-border)' }}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export const TreeRenderer: React.FC<RendererProps> = ({ renderChildren }) => (
  <div style={{ paddingLeft: 12 }}>{renderChildren()}</div>
)

export const ToolbarDecoratorRenderer: React.FC<RendererProps> = ({ renderChildren }) => (
  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>{renderChildren()}</div>
)
