import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'
import api from '../../api'
import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../../api', () => ({ default: { post: vi.fn() } }))
vi.mock('axios', () => ({ default: { post: vi.fn() } }))

describe('auth store', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        localStorage.clear()
        vi.clearAllMocks()
    })

    it('login sets tokens and username', async () => {
        ; (api.post as any).mockResolvedValue({ data: { accessToken: 'a1', refreshToken: 'r1', expiresInMinutes: 60 } })
        const store = useAuthStore()
        await store.login('u', 'p')
        expect(store.accessToken).toBe('a1')
        expect(store.refreshToken).toBe('r1')
        expect(localStorage.getItem('accessToken')).toBe('a1')
        expect(localStorage.getItem('refreshToken')).toBe('r1')
    })

    it('refresh rotates tokens', async () => {
        ; (axios.post as any).mockResolvedValue({ data: { accessToken: 'a2', refreshToken: 'r2', expiresInMinutes: 120 } })
        const store = useAuthStore()
        store.refreshToken = 'r1'
        const newAccess = await store.refresh()
        expect(newAccess).toBe('a2')
        expect(store.accessToken).toBe('a2')
        expect(localStorage.getItem('refreshToken')).toBe('r2')
    })

    it('logout clears tokens', () => {
        ; (api.post as any).mockResolvedValue({ data: {} })
        const store = useAuthStore()
        store.setTokens('a', 'r', 60)
        store.logout()
        expect(store.accessToken).toBeNull()
        expect(localStorage.getItem('accessToken')).toBeNull()
        expect(localStorage.getItem('refreshToken')).toBeNull()
    })
})
