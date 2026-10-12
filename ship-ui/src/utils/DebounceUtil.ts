/**
 * 防抖工具：搜索框输入即查时避免每个字符都发请求
 */
export const debounce = <T extends (...args: any[]) => void>(fn: T, delay = 400) => {
    let timer: ReturnType<typeof setTimeout> | undefined
    return (...args: Parameters<T>): void => {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => {
            timer = undefined
            fn(...args)
        }, delay)
    }
}
