import './CriteriaList.css'

interface CriteriaItem {
  name: string
  trigger: string
}

export function CriteriaList({ criteria }: { criteria: CriteriaItem[] }) {
  return (
    <div className="criteria-list">
      <h4 className="criteria-list__title">条件 ({criteria.length})</h4>
      {criteria.map((item) => (
        <div key={item.name} className="criteria-item">
          <span className="criteria-item__name">{item.name}</span>
          {item.trigger && <span className="criteria-item__trigger">{item.trigger}</span>}
        </div>
      ))}
    </div>
  )
}
