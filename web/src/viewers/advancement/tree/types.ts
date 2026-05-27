export interface AdvancementDisplay {
  title?: string | { text?: string; translate?: string }
  description?: string | { text?: string; translate?: string }
  icon?: { id?: string; item?: string }
  frame?: 'task' | 'goal' | 'challenge'
  hidden?: boolean
}

export interface AdvancementJson {
  display?: AdvancementDisplay
  parent?: string
  criteria?: Record<string, unknown>
  rewards?: unknown
}

export interface AdvancementTreeNode {
  id: string
  title: string
  description: string
  frame: 'task' | 'goal' | 'challenge'
  icon: string
  children: AdvancementTreeNode[]
}

export interface AdvancementTreeData {
  advancements: Record<string, AdvancementJson>
}
