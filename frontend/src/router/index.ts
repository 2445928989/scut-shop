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
import Addresses from '../pages/Addresses.vue'
import AdminSalesUsers from '../pages/AdminSalesUsers.vue'
import SalesProducts from '../pages/SalesProducts.vue'
import SalesOrders from '../pages/SalesOrders.vue'
import Dashboard from '../pages/Dashboard.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
    { path: '/', component: Products },
    { path: '/product/:id', component: ProductDetail, props: true },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/activate', component: Activate },
    { path: '/cart', component: Cart },
    { path: '/orders', component: Orders },
    { path: '/addresses', component: Addresses },
    { path: '/sales/products', component: SalesProducts },
    { path: '/sales/orders', component: SalesOrders },
    { path: '/admin/products', component: AdminProducts },
    { path: '/admin/orders', component: AdminOrders },
    { path: '/admin/reports', component: AdminReports },
    { path: '/admin/users', component: AdminUsers },
    { path: '/admin/sales', component: AdminSalesUsers },
    { path: '/admin/dashboard', component: Dashboard }
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
        if (auth.accessToken && (!auth.authorities || auth.authorities.length === 0)) {
            try { await auth.fetchMe() } catch (e) { /* ignore */ }
        }
    }
    // sales routes protection
    if (to.path.startsWith('/sales')) {
        const auth = useAuthStore()
        if (auth.accessToken && (!auth.authorities || auth.authorities.length === 0)) {
            try { await auth.fetchMe() } catch (e) { /* ignore */ }
        }
    }
    // addresses require auth
    if (to.path.startsWith('/addresses') && !isAuth) {
        return next('/login')
    }
    next()
})

export default router
