import { useState } from 'react'
import { useBridgeMessage, sendToPlugin } from '../bridge'
import type { DocumentUpdatePayload } from '../bridge'
import { StatusPage } from './StatusPage'
import { RawJsonView } from './RawJsonView'
import { getViewer } from './registry'

type ViewerState =
  | { status: 'waiting' }
  | { status: 'json-error'; message: string }
  | { status: 'not-datapack' }
  | { status: 'unsupported'; type: string }
  | { status: 'ready'; type: string; json: object; rawText: string; fileName: string }

export function ViewerApp() {
  const [state, setState] = useState<ViewerState>({ status: 'waiting' })
  const [showRaw, setShowRaw] = useState(false)

  useBridgeMessage<DocumentUpdatePayload>('DOCUMENT_UPDATE', (payload) => {
    const { text, filePath, resourceType } = payload

    if (!resourceType) {
      setState({ status: 'not-datapack' })
      return
    }

    try {
      const json = JSON.parse(text)
      if (typeof json !== 'object' || json === null || Array.isArray(json)) {
        setState({ status: 'json-error', message: 'JSON 必须是对象类型' })
        return
      }
      const fileName = filePath.split(/[/\\]/).pop() ?? ''
      setState({ status: 'ready', type: resourceType, json, rawText: text, fileName })
    } catch (e) {
      setState({ status: 'json-error', message: (e as Error).message })
    }
  })

  sendToPlugin('VIEWER_READY')

  if (state.status === 'waiting') {
    return <StatusPage title="正在连接编辑器..." />
  }
  if (state.status === 'json-error') {
    return <StatusPage title="JSON 解析错误" error={state.message} />
  }
  if (state.status === 'not-datapack') {
    return <StatusPage title="此文件不在数据包目录中" subtitle="文件路径需包含 data/<namespace>/<type>/..." />
  }
  if (state.status === 'unsupported') {
    return <StatusPage title={`「${state.type}」暂无可视化支持`} />
  }

  if (showRaw) {
    return <RawJsonView text={state.rawText} onToggle={() => setShowRaw(false)} />
  }

  const Viewer = getViewer(state.type)
  if (!Viewer) {
    return <StatusPage title={`「${state.type}」暂无可视化支持`} />
  }

  return <Viewer json={state.json} rawText={state.rawText} fileName={state.fileName} />
}
