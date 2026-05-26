interface RawJsonViewProps {
  text: string
  onToggle: () => void
}

export function RawJsonView({ text, onToggle }: RawJsonViewProps) {
  return (
    <div style={{ padding: 12 }}>
      <button
        onClick={onToggle}
        style={{
          marginBottom: 8,
          fontSize: 11,
          cursor: 'pointer',
          background: 'var(--mdt-card-bg)',
          border: '1px solid var(--mdt-border)',
          borderRadius: 4,
          padding: '2px 8px',
          color: 'var(--mdt-fg)',
        }}
      >
        返回预览
      </button>
      <pre style={{ fontSize: 11, whiteSpace: 'pre-wrap', wordBreak: 'break-all', color: 'var(--mdt-fg)' }}>
        {text}
      </pre>
    </div>
  )
}
