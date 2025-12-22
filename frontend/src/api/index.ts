import axios from 'axios'

// Use relative API base by default so browser requests go through the nginx proxy (/api)
const API_BASE = import.meta.env.VITE_API_BASE || ''
console.log('API_BASE (client):', API_BASE)
const instance = axios.create({
    baseURL: API_BASE
})

// Debugging: log requests and responses in dev
instance.interceptors.request.use((config) => {
    console.debug('[API] Request:', config.method, config.baseURL ? config.baseURL + config.url : config.url, config.params || config.data)
    return config
})
instance.interceptors.response.use((r) => {
    console.debug('[API] Response:', r.config ? (r.config.baseURL ? r.config.baseURL + r.config.url : r.config.url) : '', r.status, r.data)
    return r
}, (err) => {
    console.error('[API] Response error:', err && err.response ? { status: err.response.status, data: err.response.data } : err)
    return Promise.reject(err)
})

instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken')
    if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
    }
    // attach guest cart id if present and no auth header set
    const cartId = localStorage.getItem('cartId')
    if (cartId && config.headers && !config.headers.Authorization) {
        config.headers['X-Cart-Id'] = cartId
    }
    return config
})

// response interceptor: on 401 try to refresh access token using refresh token
let refreshPromise: Promise<string | null> | null = null

instance.interceptors.response.use(
    (r) => r,
    async (error) => {
        const original = error.config
        if (!original) return Promise.reject(error)
        if (error.response && error.response.status === 401 && !original._retry) {
            original._retry = true
            const refreshToken = localStorage.getItem('refreshToken')
            if (!refreshToken) return Promise.reject(error)

            if (!refreshPromise) {
                // use axios (not instance) to avoid infinite loop
                // call refresh endpoint directly (same-origin) to avoid instance interceptors
                refreshPromise = axios.post('/api/auth/refresh', { refreshToken })
                    .then(res => {
                        const newAccess = res.data.accessToken
                        const newRefresh = res.data.refreshToken
                        if (newAccess) localStorage.setItem('accessToken', newAccess)
                        if (newRefresh) localStorage.setItem('refreshToken', newRefresh)
                        return newAccess
                    })
                    .catch(e => {
                        // refresh failed: clear tokens
                        localStorage.removeItem('accessToken')
                        localStorage.removeItem('refreshToken')
                        throw e
                    })
                    .finally(() => { refreshPromise = null })
            }

            try {
                const newAccess = await refreshPromise
                if (newAccess) {
                    original.headers = original.headers || {}
                    original.headers.Authorization = `Bearer ${newAccess}`
                    return instance(original)
                }
            } catch (e) {
                return Promise.reject(e)
            }
        }
        return Promise.reject(error)
    }
)

export default instance
