import type { AdvancementJson, AdvancementTreeNode, AdvancementTreeData } from './types'

function extractText(value: string | { text?: string; translate?: string } | undefined): string {
  if (!value) return ''
  if (typeof value === 'string') return value
  return value.text ?? value.translate ?? ''
}

function toNode(id: string, adv: AdvancementJson): AdvancementTreeNode {
  const display = adv.display
  return {
    id,
    title: extractText(display?.title) || id.split('/').pop()?.replace('.json', '') || id,
    description: extractText(display?.description),
    frame: display?.frame ?? 'task',
    icon: display?.icon?.id ?? display?.icon?.item ?? '',
    children: [],
  }
}

/**
 * 从扁平的 advancements map 构建树结构。
 * 优化点：使用完整资源路径做 key，支持多根节点，子节点按 id 排序。
 */
export function buildAdvancementTree(data: AdvancementTreeData): AdvancementTreeNode[] {
  const { advancements } = data
  const nodeMap = new Map<string, AdvancementTreeNode>()

  for (const [id, adv] of Object.entries(advancements)) {
    nodeMap.set(id, toNode(id, adv))
  }

  const roots: AdvancementTreeNode[] = []

  for (const [id, adv] of Object.entries(advancements)) {
    const node = nodeMap.get(id)!
    const parentId = adv.parent

    if (!parentId) {
      roots.push(node)
      continue
    }

    const parentNode = nodeMap.get(parentId)
    if (parentNode) {
      parentNode.children.push(node)
    } else {
      roots.push(node)
    }
  }

  const sortChildren = (node: AdvancementTreeNode) => {
    node.children.sort((a, b) => a.id.localeCompare(b.id))
    node.children.forEach(sortChildren)
  }
  roots.sort((a, b) => a.id.localeCompare(b.id))
  roots.forEach(sortChildren)

  return roots
}
