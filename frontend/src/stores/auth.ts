import { defineStore } from 'pinia'
import api from '../api'
import axios from 'axios'

// default to same origin; docker build can override VITE_API_BASE when necessary
const API_BASE = (import.meta.env.VITE_API_BASE || '')

export const useAuthStore = defineStore('auth', {
    state: () => ({
        accessToken: (localStorage.getItem('accessToken') as string | null) || null,
        refreshToken: (localStorage.getItem('refreshToken') as string | null) || null,
        username: (localStorage.getItem('username') as string | null) || null,
        expiresIn: null as number | null,
        authorities: [] as string[]
    }),
    actions: {
        setTokens(access: string | null, refresh: string | null, expiresIn: number | null = null) {
            this.accessToken = access
            this.refreshToken = refresh
            this.expiresIn = expiresIn
            if (access) localStorage.setItem('accessToken', access)
            else localStorage.removeItem('accessToken')
            if (refresh) localStorage.setItem('refreshToken', refresh)
            else localStorage.removeItem('refreshToken')
            if (this.username) localStorage.setItem('username', this.username)
        },
        async login(username: string, password: string) {
            const r = await api.post('/api/auth/login', { username, password })
            this.setTokens(r.data.accessToken, r.data.refreshToken, r.data.expiresInMinutes)
            this.username = username
            localStorage.setItem('username', username)
            // fetch current user to populate authorities/isAdmin
            try { await this.fetchMe() } catch (e) { /* ignore */ }
        },
        async register(username: string, email: string, password: string) {
            try {
                const r = await api.post('/api/auth/register', { username, email, password })
                // if activation required, backend will indicate activation sent
                if (r.data && r.data.activation === 'sent') {
                    return r.data
                }
                // auto-login after successful register
                await this.login(username, password)
                return { status: 'registered' }
            } catch (err: any) {
                // Map backend errors (e.g., email_exists, username_exists) into a simple object
                if (err && err.response && err.response.data && err.response.data.error) {
                    return { error: err.response.data.error }
                }
                // Re-throw unexpected errors so callers/tests can handle them
                throw err
            }
        },

        async refresh() {
            if (!this.refreshToken) throw new Error('no_refresh_token')
            const r = await axios.post('/api/auth/refresh', { refreshToken: this.refreshToken })
            this.setTokens(r.data.accessToken, r.data.refreshToken, r.data.expiresInMinutes)
            // refresh user authorities after rotating token
            try { await this.fetchMe() } catch (e) { /* ignore */ }
            return r.data.accessToken
        },

        async fetchMe() {
            // populate authorities from /api/auth/me
            try {
                const r = await api.get('/api/auth/me')
                const auths = r.data && r.data.authorities ? r.data.authorities.map((a: any) => a.authority ? a.authority : a) : []
                this.authorities = auths
                return auths
            } catch (e: any) {
                this.authorities = []
                return []
            }
        },

        logout() {
            // best-effort revoke on server
            try { api.post('/api/auth/logout', { refreshToken: this.refreshToken }) } catch (e) { /* ignore */ }
            this.setTokens(null, null, null)
            this.username = null
            this.authorities = []
            localStorage.removeItem('username')
        }
    },
    getters: {
        isAdmin: (state) => state.authorities.includes('ROLE_ADMIN')
    }
})
