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
        expiresIn: null as number | null
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
        },
        async register(username: string, email: string, password: string) {
            await api.post('/api/auth/register', { username, email, password })
            // auto-login after successful register
            await this.login(username, password)
        },
        async refresh() {
            if (!this.refreshToken) throw new Error('no_refresh_token')
            const r = await axios.post('/api/auth/refresh', { refreshToken: this.refreshToken })
            this.setTokens(r.data.accessToken, r.data.refreshToken, r.data.expiresInMinutes)
            return r.data.accessToken
        },
        logout() {
            // best-effort revoke on server
            try { api.post('/api/auth/logout', { refreshToken: this.refreshToken }) } catch (e) { /* ignore */ }
            this.setTokens(null, null, null)
            this.username = null
            localStorage.removeItem('username')
        }
    }
})
