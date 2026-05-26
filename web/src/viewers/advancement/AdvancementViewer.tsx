import type { ViewerProps } from '../registry'
import { AdvancementCard } from './components/AdvancementCard'
import { CriteriaList } from './components/CriteriaList'
import { RewardList } from './components/RewardList'
import './advancement.css'

interface AdvancementJson {
  display?: {
    title?: string | { text?: string; translate?: string }
    description?: string | { text?: string; translate?: string }
    icon?: { id?: string; item?: string }
    frame?: string
    hidden?: boolean
  }
  parent?: string
  criteria?: Record<string, { trigger?: string; conditions?: object }>
  rewards?: {
    recipes?: string[]
    loot?: string[]
    experience?: number
    function?: string
  }
}

function extractText(value: string | { text?: string; translate?: string } | undefined): string {
  if (!value) return ''
  if (typeof value === 'string') return value
  return value.text ?? value.translate ?? ''
}

export function AdvancementViewer({ json, fileName }: ViewerProps) {
  const adv = json as AdvancementJson
  const display = adv.display
  const title = extractText(display?.title) || fileName.replace('.json', '')
  const description = extractText(display?.description)
  const frame = display?.frame ?? 'task'
  const icon = display?.icon?.id ?? display?.icon?.item ?? ''
  const parent = adv.parent
  const hidden = display?.hidden ?? false

  const criteria = adv.criteria
    ? Object.entries(adv.criteria).map(([name, def]) => ({
        name,
        trigger: def.trigger ?? '',
      }))
    : []

  const rewards = adv.rewards

  return (
    <div className="advancement-viewer">
      <AdvancementCard
        title={title}
        description={description}
        frame={frame}
        icon={icon}
        parent={parent}
        hidden={hidden}
      />
      {criteria.length > 0 && <CriteriaList criteria={criteria} />}
      {rewards && <RewardList rewards={rewards} />}
    </div>
  )
}
