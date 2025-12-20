import { createRouter, createWebHistory } from 'vue-router'
import Products from '../pages/Products.vue'
import ProductDetail from '../pages/ProductDetail.vue'
import Login from '../pages/Login.vue'
import Cart from '../pages/Cart.vue'
import Register from '../pages/Register.vue'
import Activate from '../pages/Activate.vue'

const routes = [
    { path: '/', component: Products },
    { path: '/product/:id', component: ProductDetail, props: true },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/activate', component: Activate },
    { path: '/cart', component: Cart }
]

const router = createRouter({ history: createWebHistory(), routes })

// Prevent authenticated users from visiting login/register pages
router.beforeEach((to, from, next) => {
    const isAuth = !!localStorage.getItem('accessToken')
    if ((to.path === '/login' || to.path === '/register') && isAuth) {
        return next('/')
    }
    next()
})

export default router
