import type { PatchOperation, StatePatch } from '../types/protocol'

type Listener = () => void

export class StateStore {
  private state: Record<string, unknown> = {}
  private version = 0
  private listeners: Set<Listener> = new Set()

  getState(): Record<string, unknown> { return this.state }
  getVersion(): number { return this.version }

  getValue(path: string): unknown {
    const keys = path.replace(/^\//, '').split('/')
    let current: unknown = this.state
    for (const key of keys) {
      if (current == null || typeof current !== 'object') return undefined
      current = (current as Record<string, unknown>)[key]
    }
    return current
  }

  applySnapshot(state: Record<string, unknown>, baseVersion: number) {
    this.state = structuredClone(state)
    this.version = baseVersion
    this.notify()
  }

  applyPatch(patch: StatePatch): boolean {
    if (patch.baseVersion !== this.version) return false
    for (const op of patch.patch) {
      this.applyOperation(op)
    }
    this.version = patch.baseVersion + 1
    this.notify()
    return true
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener)
    return () => { this.listeners.delete(listener) }
  }

  private applyOperation(op: PatchOperation) {
    const keys = op.path.replace(/^\//, '').split('/')
    const lastKey = keys.pop()
    if (!lastKey) return

    let parent: unknown = this.state
    for (const key of keys) {
      if (parent == null || typeof parent !== 'object') return
      parent = (parent as Record<string, unknown>)[key]
    }
    if (parent == null || typeof parent !== 'object') return

    const obj = parent as Record<string, unknown>
    switch (op.op) {
      case 'add':
      case 'replace':
        obj[lastKey] = structuredClone(op.value)
        break
      case 'remove':
        delete obj[lastKey]
        break
    }
  }

  private notify() {
    this.listeners.forEach(fn => fn())
  }
}
