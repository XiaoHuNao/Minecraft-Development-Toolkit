import type { BridgeMessage } from '../bridge/types'

const defaultDarkTheme = {
  bg: '#2b2b2b',
  fg: '#d4d4d4',
  border: '#3c3c3c',
  accent: '#589df6',
  cardBg: '#333333',
  cardHover: '#3a3a3a',
  muted: '#777777',
  error: '#ef5350',
}

export function installMockBridge() {
  const params = new URLSearchParams(location.search)
  const mode = params.get('mode') ?? 'viewer'

  function send(msg: BridgeMessage) {
    window.__MDT_BRIDGE__?.receive(JSON.stringify(msg))
  }

  setTimeout(() => {
    send({ type: 'MODE_SET', payload: { mode }, timestamp: Date.now() })
    send({ type: 'THEME_UPDATE', payload: defaultDarkTheme, timestamp: Date.now() })

    if (mode === 'viewer') {
      import('./fixtures/advancement.json').then((json) => {
        send({
          type: 'DOCUMENT_UPDATE',
          payload: {
            text: JSON.stringify(json.default ?? json, null, 2),
            resourceType: 'ADVANCEMENT',
            filePath: 'data/minecraft/advancement/story/mine_diamond.json',
          },
          timestamp: Date.now(),
        })
      })
    }

    if (mode === 'toolwindow') {
      send({
        type: 'CONNECTION_PARAMS',
        payload: { host: 'localhost', port: 8765, token: '' },
        timestamp: Date.now(),
      })
    }
  }, 100)
}
