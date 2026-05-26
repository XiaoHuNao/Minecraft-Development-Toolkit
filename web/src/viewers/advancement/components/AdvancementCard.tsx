interface AdvancementCardProps {
  title: string
  description: string
  frame: string
  icon: string
  parent?: string
  hidden: boolean
}

export function AdvancementCard({ title, description, frame, icon, parent, hidden }: AdvancementCardProps) {
  const frameClass = `advancement-card advancement-card--${frame}`

  return (
    <div className={frameClass}>
      <div className="advancement-card__header">
        <div className="advancement-card__icon">
          <span className="advancement-card__icon-text">{icon.split(':').pop() ?? '?'}</span>
        </div>
        <div className="advancement-card__info">
          <h3 className="advancement-card__title">{title}</h3>
          {description && <p className="advancement-card__desc">{description}</p>}
        </div>
        <span className="advancement-card__frame">{frame}</span>
      </div>
      {(parent || hidden) && (
        <div className="advancement-card__meta">
          {parent && <span className="advancement-card__parent">parent: {parent}</span>}
          {hidden && <span className="advancement-card__hidden">hidden</span>}
        </div>
      )}
    </div>
  )
}
