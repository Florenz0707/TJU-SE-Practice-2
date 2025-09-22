import { ref, onMounted, onUnmounted, type Ref } from 'vue'

/**
 * A Vue Composition API function (Composable) for managing WebSocket connections.
 * @param {string} url The WebSocket server URL.
 */
export function useWebSocket(url: string) {
  const ws: Ref<WebSocket | null> = ref(null)
  const isConnected: Ref<boolean> = ref(false)
  const message: Ref<any> = ref(null) // Message can be of any type
  const error: Ref<Event | null> = ref(null)

  let reconnectTimer: number | null = null
  const reconnectInterval = 5000 // Reconnect every 5 seconds

  const connect = () => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      return
    }

    console.log(`[WebSocket] Connecting to ${url}...`)
    ws.value = new WebSocket(url)

    ws.value.onopen = () => {
      console.log('[WebSocket] Connection successful!')
      isConnected.value = true
      error.value = null
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }

    ws.value.onmessage = (event: MessageEvent) => {
      console.log('[WebSocket] Message received:', event.data)
      try {
        message.value = JSON.parse(event.data)
      } catch (e) {
        message.value = event.data // Assign as raw data if not JSON
      }
    }

    ws.value.onerror = (err: Event) => {
      console.error('[WebSocket] Error occurred:', err)
      error.value = err
    }

    ws.value.onclose = (event: CloseEvent) => {
      console.log('[WebSocket] Connection closed:', event)
      isConnected.value = false
      ws.value = null

      if (!event.wasClean) {
        console.log(
          `[WebSocket] Connection lost. Reconnecting in ${reconnectInterval / 1000} seconds...`
        )
        if (!reconnectTimer) {
          reconnectTimer = setTimeout(connect, reconnectInterval)
        }
      }
    }
  }

  const disconnect = () => {
    if (ws.value) {
      console.log('[WebSocket] Disconnecting manually.')
      ws.value.close()
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const sendMessage = (data: any) => {
    if (ws.value && isConnected.value) {
      ws.value.send(JSON.stringify(data))
    } else {
      console.warn('[WebSocket] Not connected. Cannot send message.')
    }
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return {
    isConnected,
    message,
    error,
    sendMessage,
    connect,
    disconnect,
  }
}
