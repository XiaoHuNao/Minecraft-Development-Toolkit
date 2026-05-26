import { useState } from 'react'
import type { RendererProps } from '../ComponentRegistry'

export const TextFieldRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const placeholder = (node.props['placeholder'] as string) ?? ''
  const binding = node.bindings.find(b => b.prop === 'value')
  const initial = binding ? (stateStore.getValue(binding.path) as string ?? '') : (node.props['value'] as string ?? '')
  const [value, setValue] = useState(initial)

  return (
    <input
      type="text"
      value={value}
      placeholder={placeholder}
      onChange={(e) => { setValue(e.target.value); onEvent(node.id, 'change', e.target.value) }}
      style={{
        padding: '4px 8px', fontSize: 12, border: '1px solid var(--mdt-border)',
        borderRadius: 4, background: 'var(--mdt-bg)', color: 'var(--mdt-fg)', width: '100%',
      }}
    />
  )
}

export const TextAreaRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const placeholder = (node.props['placeholder'] as string) ?? ''
  const rows = (node.props['rows'] as number) ?? 3
  const binding = node.bindings.find(b => b.prop === 'value')
  const initial = binding ? (stateStore.getValue(binding.path) as string ?? '') : (node.props['value'] as string ?? '')
  const [value, setValue] = useState(initial)

  return (
    <textarea
      value={value}
      placeholder={placeholder}
      rows={rows}
      onChange={(e) => { setValue(e.target.value); onEvent(node.id, 'change', e.target.value) }}
      style={{
        padding: '4px 8px', fontSize: 12, border: '1px solid var(--mdt-border)',
        borderRadius: 4, background: 'var(--mdt-bg)', color: 'var(--mdt-fg)', width: '100%', resize: 'vertical',
      }}
    />
  )
}

export const CheckboxRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const label = (node.props['label'] as string) ?? ''
  const binding = node.bindings.find(b => b.prop === 'checked')
  const initial = binding ? (stateStore.getValue(binding.path) as boolean ?? false) : (node.props['checked'] as boolean ?? false)
  const [checked, setChecked] = useState(initial)

  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer' }}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => { setChecked(e.target.checked); onEvent(node.id, 'change', e.target.checked) }}
      />
      {label}
    </label>
  )
}

export const DropdownRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const options = (node.props['options'] as string[]) ?? []
  const binding = node.bindings.find(b => b.prop === 'selected')
  const initial = binding ? (stateStore.getValue(binding.path) as string ?? '') : (node.props['selected'] as string ?? '')
  const [selected, setSelected] = useState(initial)

  return (
    <select
      value={selected}
      onChange={(e) => { setSelected(e.target.value); onEvent(node.id, 'change', e.target.value) }}
      style={{
        padding: '4px 8px', fontSize: 12, border: '1px solid var(--mdt-border)',
        borderRadius: 4, background: 'var(--mdt-bg)', color: 'var(--mdt-fg)',
      }}
    >
      {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
    </select>
  )
}

export const SliderRenderer: React.FC<RendererProps> = ({ node, stateStore, onEvent }) => {
  const min = (node.props['min'] as number) ?? 0
  const max = (node.props['max'] as number) ?? 100
  const step = (node.props['step'] as number) ?? 1
  const binding = node.bindings.find(b => b.prop === 'value')
  const initial = binding ? (stateStore.getValue(binding.path) as number ?? min) : (node.props['value'] as number ?? min)
  const [value, setValue] = useState(initial)

  return (
    <input
      type="range" min={min} max={max} step={step} value={value}
      onChange={(e) => { const v = Number(e.target.value); setValue(v); onEvent(node.id, 'change', v) }}
      style={{ width: '100%' }}
    />
  )
}
