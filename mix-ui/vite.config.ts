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
