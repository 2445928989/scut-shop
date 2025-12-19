import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', {
    state: () => ({ items: [] as Array<{ productId: number, quantity: number }> }),
    actions: {
        add(productId: number, quantity: number) {
            const found = this.items.find(i => i.productId === productId)
            if (found) found.quantity += quantity
            else this.items.push({ productId, quantity })
        },
        setItems(items: Array<{ productId: number, quantity: number }>) {
            this.items = items.map(i => ({ productId: i.productId, quantity: i.quantity }))
        },
        clear() { this.items = [] }
    }
})
