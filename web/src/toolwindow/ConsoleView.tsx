import { useRef, useState } from 'react'

interface ConsoleEntry {
  level: 'INFO' | 'WARN' | 'ERROR' | 'COMMAND'
  message: string
}

interface ConsoleViewProps {
  entries: ConsoleEntry[]
  onCommand: (command: string) => void
  onTabComplete: (input: string, cursor: number) => void
  suggestions: string[]
  onSuggestionSelect: (suggestion: string) => void
}

export function ConsoleView({ entries, onCommand, onTabComplete, suggestions, onSuggestionSelect }: ConsoleViewProps) {
  const [input, setInput] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const logRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && input.trim()) {
      onCommand(input.trim())
      setInput('')
      setShowSuggestions(false)
    } else if (e.key === 'Tab') {
      e.preventDefault()
      if (input.trim()) {
        onTabComplete(input, inputRef.current?.selectionStart ?? input.length)
        setShowSuggestions(true)
      }
    } else if (e.key === 'Escape') {
      setShowSuggestions(false)
    }
  }

  const levelColor = (level: string) => {
    switch (level) {
      case 'ERROR': return 'var(--mdt-error)'
      case 'WARN': return '#ff9800'
      case 'COMMAND': return 'var(--mdt-accent)'
      default: return 'var(--mdt-fg)'
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div ref={logRef} style={{ flex: 1, overflow: 'auto', padding: 8, fontFamily: 'monospace', fontSize: 11 }}>
        {entries.map((entry, i) => (
          <div key={i} style={{ color: levelColor(entry.level), padding: '1px 0', whiteSpace: 'pre-wrap' }}>
            {entry.level === 'COMMAND' ? `> ${entry.message}` : `[${entry.level}] ${entry.message}`}
          </div>
        ))}
      </div>
      <div style={{ position: 'relative', borderTop: '1px solid var(--mdt-border)' }}>
        {showSuggestions && suggestions.length > 0 && (
          <div style={{
            position: 'absolute', bottom: '100%', left: 0, right: 0,
            background: 'var(--mdt-card-bg)', border: '1px solid var(--mdt-border)',
            borderRadius: '4px 4px 0 0', maxHeight: 120, overflow: 'auto',
          }}>
            {suggestions.map((s, i) => (
              <div
                key={i}
                onClick={() => { onSuggestionSelect(s); setShowSuggestions(false) }}
                style={{ padding: '4px 8px', fontSize: 11, cursor: 'pointer', fontFamily: 'monospace' }}
                onMouseEnter={(e) => { (e.target as HTMLElement).style.background = 'var(--mdt-card-hover)' }}
                onMouseLeave={(e) => { (e.target as HTMLElement).style.background = '' }}
              >
                {s}
              </div>
            ))}
          </div>
        )}
        <input
          ref={inputRef}
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入指令按 Enter 发送，按 Tab 补全"
          style={{
            width: '100%', padding: '6px 8px', fontSize: 12, fontFamily: 'monospace',
            border: 'none', background: 'var(--mdt-bg)', color: 'var(--mdt-fg)', outline: 'none',
          }}
        />
      </div>
    </div>
  )
}
