import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue({
        template: {
            compilerOptions: {
                comments: false, // 生成的文件中，去除注释
            },
        },
    })
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    build: {
        // html2pdf.js（含 html2canvas / jsPDF）压缩后约 1MB，已改为导出 PDF 时动态加载（d-rx.vue），
        // 首屏不再下载；其余依赖按用途拆包，避免单个 chunk 过大阻塞首屏。
        // 阈值 1000 只放宽给这个按需加载的第三方大库，其余包由 manualChunks 拆散。
        chunkSizeWarningLimit: 1000,
        rollupOptions: {
            output: {
                manualChunks(id: string): string | undefined {
                    if (!id.includes('node_modules')) return undefined
                    if (/html2pdf|html2canvas|jspdf|dompurify|canvg/.test(id)) return 'pdf'
                    if (/echarts|zrender/.test(id)) return 'charts'
                    if (/element-plus|@element-plus/.test(id)) return 'element'
                    if (/\/vue\/|@vue\/|vue-router|pinia/.test(id)) return 'framework'
                    return 'vendor'
                }
            }
        }
    },
    server: {
        host: '0.0.0.0',
        port: 3120, // 端口号
        proxy: {  // 跨域代理
            '/api': {
                target: "http://localhost:8085",
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '')
            },
            // 在线咨询 WebSocket：开发时由 vite 反代到后端 8085 的 /chat 端点
            '/chat': {
                target: 'ws://localhost:8085',
                ws: true,
                changeOrigin: true,
                rewrite: (path) => path
            }
        }
    },

})
