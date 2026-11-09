import {App as Vpp, createApp} from 'vue'
import './style.css'
// 统一报告设计语言（所有可打印文书共用，只注入一次）
import './styles/report-modern.scss'
import App from './App.vue'
// 路由
import router from './router'
// ElementPlus
import {pushElement} from "@/push-element";

const app: Vpp = createApp(App)
app.use(router)
pushElement(app)
app.mount('#app')
