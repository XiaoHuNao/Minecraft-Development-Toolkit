import { registerViewer } from '../../registry'
import { AdvancementTreeViewer } from './AdvancementTreeViewer'

registerViewer('ADVANCEMENT_TREE', AdvancementTreeViewer)

export { AdvancementTreeViewer }
export { buildAdvancementTree } from './buildTree'
export type { AdvancementTreeNode, AdvancementTreeData } from './types'
