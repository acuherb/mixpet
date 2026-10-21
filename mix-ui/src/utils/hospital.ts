import {ref, watch} from 'vue'
import {reqHospitalSetting} from "@/request/SettingApi";
import {emptyHospital, HospitalSetting} from "@/model/entiy/HospitalSetting";

/**
 * 医院信息全局单例（贴面场景：页脚/首页/title 缓存一次，App 启动加载）
 * 注意：打印页（window.open/独立渲染）不用此缓存，自行拉接口保证取到最新值
 */
export const hospital = ref<HospitalSetting>(emptyHospital())

/** 默认品牌色（与 style.css :root 保持一致） */
const DEFAULT_BRAND = '#1E6FA8'

/** HEX 颜色压暗/提亮（用于派生 --brand-dark） */
function shade(hex: string, ratio: number): string {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!m) return DEFAULT_BRAND
  const n = parseInt(m[1], 16)
  const clamp = (v: number) => Math.max(0, Math.min(255, Math.round(v)))
  const r = clamp(((n >> 16) & 255) * ratio)
  const g = clamp(((n >> 8) & 255) * ratio)
  const b = clamp((n & 255) * ratio)
  return '#' + [r, g, b].map(v => v.toString(16).padStart(2, '0')).join('')
}

/** HEX 与白色混合（ratio=白占比），用于派生极淡的背景/软色 */
function mixWhite(hex: string, ratio: number): string {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!m) return DEFAULT_BRAND
  const n = parseInt(m[1], 16)
  const ch = [(n >> 16) & 255, (n >> 8) & 255, n & 255]
      .map(c => Math.round(c * (1 - ratio) + 255 * ratio))
  return '#' + ch.map(v => v.toString(16).padStart(2, '0')).join('')
}

/**
 * 把品牌色注入 CSS 变量，全站引用处即时生效：
 * --brand 主色 / --brand-dark 深色(hover) / --bg 页面背景(极淡) / --brand-soft 软色(页脚等)
 * 支持 HEX（#RRGGBB）；非法值回落默认色
 */
export const injectBrand = (color?: string | null): void => {
  const brand = /^#[0-9a-fA-F]{6}$/.test((color || '').trim()) ? color!.trim() : DEFAULT_BRAND
  const root = document.documentElement
  root.style.setProperty('--brand', brand)
  root.style.setProperty('--brand-dark', shade(brand, 0.82))
  root.style.setProperty('--bg', mixWhite(brand, 0.93))
  root.style.setProperty('--brand-soft', mixWhite(brand, 0.86))
}

let loading: Promise<void> | null = null

/** 加载医院信息（已加载则跳过；force=true 强制刷新） */
export const loadHospital = (force = false): Promise<void> => {
    if (loading && !force) return loading
    loading = reqHospitalSetting().then(s => {
        if (s) hospital.value = s
    }).catch(() => { /* 接口异常时保持默认值，不阻断页面 */ })
    return loading
}

// 品牌色联动：医院信息变化（启动加载/设置页保存）即重新注入 :root
watch(hospital, h => injectBrand(h?.brandColor), {immediate: true})
