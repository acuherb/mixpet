import {App as Vpp, createApp} from 'vue'
import './style.css'
// 统一报告设计语言（所有可打印文书共用，只注入一次）
import './styles/report-modern.scss'
import App from './App.vue'
// 路由
import router from './router'
// ElementPlus
import {pushElement} from "@/push-element";
import BackBtn from "@/components/row/BackBtn.vue";

const app: Vpp = createApp(App)
app.use(router)
pushElement(app)
// 全局业务组件：统一返回按钮（避免各模块手写返回导致样式各异）
app.component('BackBtn', BackBtn)
app.mount('#app')
