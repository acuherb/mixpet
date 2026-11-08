/** 全局共享的咨询 WebSocket：单例 + 订阅分发 + 指数退避重连 + 发送队列。 */
type Handler = (data: any) => void

const handlers = new Set<Handler>()
/** 诊疗流系统事件订阅者（type=FLOW_REFRESH），与聊天订阅者隔离 */
const flowHandlers = new Set<Handler>()
const queue: string[] = []
let ws: WebSocket | null = null
let retry = 0
let timer: number | null = null
let manualClosed = false

const wsUrl = (): string =>
    (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/chat"

const scheduleReconnect = (): void => {
    if (timer !== null || manualClosed || (handlers.size === 0 && flowHandlers.size === 0)) return
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
        // 系统事件只分发给流订阅者，绝不进入聊天消息流（否则聊天页会把刷新帧渲染成一条消息）
        const target = data && data.type === 'FLOW_REFRESH' ? flowHandlers : handlers
        target.forEach(h => {
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

/** 订阅聊天消息，返回取消订阅函数；最后一个订阅者（含流订阅者）退出时自动断开 */
export const subscribe = (handler: Handler): (() => void) => {
    handlers.add(handler)
    manualClosed = false
    connect()
    return () => {
        handlers.delete(handler)
        if (handlers.size === 0 && flowHandlers.size === 0) manualClose()
    }
}

/**
 * 订阅诊疗流系统事件（FLOW_REFRESH）。后端在挂号/接诊/开单/出结果/收款/随访成功
 * 并事务提交后广播，前端据此即时拉取流水线，替代纯轮询的滞后感。
 */
export const subscribeFlow = (handler: Handler): (() => void) => {
    flowHandlers.add(handler)
    manualClosed = false
    connect()
    return () => {
        flowHandlers.delete(handler)
        if (handlers.size === 0 && flowHandlers.size === 0) manualClose()
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
