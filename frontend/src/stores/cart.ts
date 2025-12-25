import { defineStore } from 'pinia'

export interface CartItem {
    productId: number
    quantity: number
    name?: string
    price?: number
    imageUrl?: string
    id?: number // server-side cart item id
    status?: number // 1=on_shelf, 0=off_shelf
}

export const useCartStore = defineStore('cart', {
    state: () => ({ items: [] as CartItem[] }),
    actions: {
        add(product: any, quantity: number, serverId?: number) {
            const productId = typeof product === 'number' ? product : product.id
            const found = this.items.find(i => i.productId === productId)
            if (found) {
                found.quantity += quantity
                if (serverId) found.id = serverId
            } else {
                if (typeof product === 'object') {
                    this.items.push({
                        productId: product.id,
                        quantity,
                        name: product.name,
                        price: product.price,
                        imageUrl: product.imageUrl,
                        id: serverId,
                        status: product.status
                    })
                } else {
                    this.items.push({ productId, quantity, id: serverId })
                }
            }
        },
        setItems(items: CartItem[]) {
            this.items = items.map(i => ({
                productId: i.productId,
                quantity: i.quantity,
                name: i.name,
                price: i.price,
                imageUrl: i.imageUrl,
                id: i.id,
                status: i.status
            }))
        },
        updateQuantity(productId: number, quantity: number) {
            const found = this.items.find(i => i.productId === productId)
            if (found) {
                found.quantity = quantity
            }
        },
        removeItem(productId: number) {
            this.items = this.items.filter(i => i.productId !== productId)
        },
        clear() { this.items = [] }
    },
    getters: {
        totalPrice: (state) => {
            return state.items.reduce((total, item) => {
                // Only calculate price for items that are on shelf (status === 1)
                if (item.status === 0) return total
                return total + (item.price || 0) * item.quantity
            }, 0)
        }
    }
})
