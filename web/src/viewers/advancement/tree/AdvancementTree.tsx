import type { AdvancementTreeNode } from './types'

interface TreeNodeProps {
  node: AdvancementTreeNode
}

function TreeNodeItem({ node }: TreeNodeProps) {
  const iconName = node.icon ? node.icon.replace(/^minecraft:/, '') : ''

  return (
    <li>
      <span className={`tree-node tree-node--${node.frame}`}>
        <span className="tree-node__icon">{iconName || '?'}</span>
        <div className="tree-node__tooltip">
          <div className="tree-node__tooltip-title">{node.title}</div>
          {node.description && (
            <div className="tree-node__tooltip-desc">{node.description}</div>
          )}
        </div>
      </span>
      {node.children.length > 0 && (
        <ul>
          {node.children.map((child) => (
            <TreeNodeItem key={child.id} node={child} />
          ))}
        </ul>
      )}
    </li>
  )
}

interface TreeProps {
  roots: AdvancementTreeNode[]
}

export function AdvancementTree({ roots }: TreeProps) {
  return (
    <ul>
      {roots.map((root) => (
        <TreeNodeItem key={root.id} node={root} />
      ))}
    </ul>
  )
}
