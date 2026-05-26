import { ComponentRegistry } from '../ComponentRegistry'
import { ColumnRenderer, RowRenderer, TextRenderer, ButtonRenderer } from './layout'
import { TextFieldRenderer, TextAreaRenderer, CheckboxRenderer, DropdownRenderer, SliderRenderer } from './inputs'
import { DividerRenderer, SpacerRenderer, CardRenderer, ProgressBarRenderer, LazyColumnRenderer, IconRenderer, IconButtonRenderer, BannerRenderer } from './display'
import { TabStripRenderer, MarkdownRenderer, TableRenderer, TreeRenderer, ToolbarDecoratorRenderer } from './complex'

export function createDefaultRegistry(): ComponentRegistry {
  const registry = new ComponentRegistry()
  registry.register('column', ColumnRenderer)
  registry.register('row', RowRenderer)
  registry.register('text', TextRenderer)
  registry.register('button', ButtonRenderer)
  registry.register('textField', TextFieldRenderer)
  registry.register('textArea', TextAreaRenderer)
  registry.register('checkbox', CheckboxRenderer)
  registry.register('dropdown', DropdownRenderer)
  registry.register('slider', SliderRenderer)
  registry.register('divider', DividerRenderer)
  registry.register('spacer', SpacerRenderer)
  registry.register('card', CardRenderer)
  registry.register('progressBar', ProgressBarRenderer)
  registry.register('lazyColumn', LazyColumnRenderer)
  registry.register('icon', IconRenderer)
  registry.register('iconButton', IconButtonRenderer)
  registry.register('banner', BannerRenderer)
  registry.register('tabStrip', TabStripRenderer)
  registry.register('markdown', MarkdownRenderer)
  registry.register('table', TableRenderer)
  registry.register('tree', TreeRenderer)
  registry.register('toolbarDecorator', ToolbarDecoratorRenderer)
  return registry
}
