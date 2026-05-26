import { useEffect, useRef, useState } from 'react'
import { onBridgeMessage } from './JcefBridge'
import type { BridgeMessage, BridgeMessageType } from './types'

export function useBridgeMessage<T = unknown>(
  type: BridgeMessageType,
  handler: (payload: T) => void
) {
  const handlerRef = useRef(handler)
  handlerRef.current = handler

  useEffect(() => {
    return onBridgeMessage((msg: BridgeMessage) => {
      if (msg.type === type) {
        handlerRef.current(msg.payload as T)
      }
    })
  }, [type])
}

export function useBridgeState<T>(
  type: BridgeMessageType,
  initial: T
): T {
  const [state, setState] = useState<T>(initial)
  useBridgeMessage<T>(type, setState)
  return state
}
