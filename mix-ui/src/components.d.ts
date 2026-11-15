// 全局注册组件的模板类型声明，使 vue-tsc 能识别 main.ts / push-element.ts 注册的全局组件
export {}

declare module 'vue' {
  export interface GlobalComponents {
    BackBtn: typeof import('./components/row/BackBtn.vue')['default']
  }
}
