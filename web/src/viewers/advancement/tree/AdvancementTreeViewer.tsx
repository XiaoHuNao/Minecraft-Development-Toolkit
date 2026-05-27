import type { ViewerProps } from '../../registry'
import type { AdvancementTreeData } from './types'
import { buildAdvancementTree } from './buildTree'
import { AdvancementTree } from './AdvancementTree'
import './advancement-tree.css'

export function AdvancementTreeViewer({ json }: ViewerProps) {
  const data = json as AdvancementTreeData
  const roots = buildAdvancementTree(data)

  if (roots.length === 0) {
    return <div className="advancement-tree"><p>无进度数据</p></div>
  }

  return (
    <div className="advancement-tree">
      <AdvancementTree roots={roots} />
    </div>
  )
}
