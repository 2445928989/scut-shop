import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/global.css'

const app = createApp(App)

// 全局错误处理，方便在远程服务器调试
app.config.errorHandler = (err, instance, info) => {
    console.error('Vue Global Error:', err)
    console.error('Info:', info)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
