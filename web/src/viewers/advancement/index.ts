import { registerViewer } from '../registry'
import { AdvancementViewer } from './AdvancementViewer'

registerViewer('ADVANCEMENT', AdvancementViewer)

export { AdvancementViewer }
