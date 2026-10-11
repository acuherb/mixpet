/**
 * 表格最大高度：跟随窗口高度变化
 * @param offset 固定区域高度（顶部操作栏 + 表头 + 分页栏 + padding）
 */
import {onMounted, onUnmounted, ref} from 'vue'

export const useTableMaxHeight = (offset = 240) => {
  const h = ref(window.innerHeight - offset)
  const update = () => {
    h.value = window.innerHeight - offset
  }
  onMounted(() => window.addEventListener('resize', update))
  onUnmounted(() => window.removeEventListener('resize', update))
  return h
}
