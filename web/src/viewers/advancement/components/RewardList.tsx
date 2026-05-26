import './RewardList.css'

interface Rewards {
  recipes?: string[]
  loot?: string[]
  experience?: number
  function?: string
}

export function RewardList({ rewards }: { rewards: Rewards }) {
  const items: string[] = []
  if (rewards.experience) items.push(`经验: ${rewards.experience}`)
  if (rewards.recipes) rewards.recipes.forEach((r) => items.push(`配方: ${r}`))
  if (rewards.loot) rewards.loot.forEach((l) => items.push(`战利品表: ${l}`))
  if (rewards.function) items.push(`函数: ${rewards.function}`)

  if (items.length === 0) return null

  return (
    <div className="reward-list">
      <h4 className="reward-list__title">奖励</h4>
      {items.map((item, i) => (
        <div key={i} className="reward-item">{item}</div>
      ))}
    </div>
  )
}
