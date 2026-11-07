/** 全局共享的咨询 WebSocket：单例 + 订阅分发 + 指数退避重连 + 发送队列。 */
type Handler = (data: any) => void

const handlers = new Set<Handler>()
const queue: string[] = []
let ws: WebSocket | null = null
let retry = 0
let timer: number | null = null
let manualClosed = false

const wsUrl = (): string =>
    (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/chat"

const scheduleReconnect = (): void => {
    if (timer !== null || manualClosed || handlers.size === 0) return
    const delay = Math.min(1000 * 2 ** retry, 30000)
    retry++
    timer = window.setTimeout(() => {
        timer = null
        connect()
    }, delay)
}

const connect = (): void => {
    if (manualClosed || ws) return
    try {
        ws = new WebSocket(wsUrl())
    } catch {
        scheduleReconnect()
        return
    }
    ws.onopen = () => {
        retry = 0
        while (queue.length > 0 && ws?.readyState === WebSocket.OPEN) {
            ws.send(queue.shift() as string)
        }
    }
    ws.onmessage = ev => {
        let data: any
        try {
            data = JSON.parse(ev.data)
        } catch {
            return // 忽略心跳/非 JSON 帧
        }
        handlers.forEach(h => {
            try {
                h(data)
            } catch {
                // 单个订阅者异常不影响其他订阅者
            }
        })
    }
    ws.onclose = () => {
        ws = null
        scheduleReconnect()
    }
    ws.onerror = () => {
        try {
            ws?.close()
        } catch {
            // 忽略关闭异常
        }
    }
}

/** 订阅消息，返回取消订阅函数；最后一个订阅者退出时自动断开 */
export const subscribe = (handler: Handler): (() => void) => {
    handlers.add(handler)
    manualClosed = false
    connect()
    return () => {
        handlers.delete(handler)
        if (handlers.size === 0) manualClose()
    }
}

/** 发送：未连接时先入队，连接恢复后自动补发 */
export const send = (payload: unknown): boolean => {
    const text = JSON.stringify(payload)
    if (ws?.readyState === WebSocket.OPEN) {
        ws.send(text)
        return true
    }
    if (queue.length < 50) queue.push(text)
    connect()
    return false
}

/** 主动关闭（登出/卸载），不再自动重连 */
export const manualClose = (): void => {
    manualClosed = true
    if (timer !== null) {
        clearTimeout(timer)
        timer = null
    }
    queue.length = 0
    try {
        ws?.close()
    } catch {
        // 忽略关闭异常
    }
    ws = null
}
