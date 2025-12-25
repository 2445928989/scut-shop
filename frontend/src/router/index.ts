import { createRouter, createWebHistory } from 'vue-router'
import Products from '../pages/Products.vue'
import ProductDetail from '../pages/ProductDetail.vue'
import Login from '../pages/Login.vue'
import Cart from '../pages/Cart.vue'
import Orders from '../pages/Orders.vue'
import Register from '../pages/Register.vue'
import Activate from '../pages/Activate.vue'
import AdminProducts from '../pages/AdminProducts.vue'
import AdminOrders from '../pages/AdminOrders.vue'
import AdminReports from '../pages/AdminReports.vue'
import AdminUsers from '../pages/AdminUsers.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
    { path: '/', component: Products },
    { path: '/product/:id', component: ProductDetail, props: true },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/activate', component: Activate },
    { path: '/cart', component: Cart },
    { path: '/orders', component: Orders },
    { path: '/admin/products', component: AdminProducts },
    { path: '/admin/orders', component: AdminOrders },
    { path: '/admin/reports', component: AdminReports },
    { path: '/admin/users', component: AdminUsers }
]
const router = createRouter({ history: createWebHistory(), routes })

// Prevent authenticated users from visiting login/register pages
router.beforeEach(async (to, from, next) => {
    // 修复双斜杠问题 (例如 //activate -> /activate)
    if (to.path.startsWith('//')) {
        return next(to.path.replace(/\/+/g, '/'))
    }

    const isAuth = !!localStorage.getItem('accessToken')
    // if trying to visit login/register while authenticated, redirect home
    if ((to.path === '/login' || to.path === '/register') && isAuth) {
        return next('/')
    }
    // admin routes protection: require ROLE_ADMIN
    if (to.path.startsWith('/admin')) {
        const auth = useAuthStore()
        // if token present but authorities not yet populated, fetch them (best-effort)
        if (auth.accessToken && (!auth.authorities || auth.authorities.length === 0)) {
            try { await auth.fetchMe() } catch (e) { /* ignore */ }
        }
        // 如果用户没有权限，也允许访问（让页面组件自己处理显示登录提示）
        // 而不是立即跳转，这样登出后用户可以留在当前页面
    }
    next()
})

export default router
