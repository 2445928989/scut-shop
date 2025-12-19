<template>
  <div>
    <h3>购物车</h3>
    <div v-if="items.length===0">购物车为空</div>
    <div v-else>
      <div v-for="it in items" :key="it.productId">商品ID: {{it.productId}} 数量: {{it.quantity}}</div>
      <el-button type="primary" @click="checkout">结账</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useCartStore } from '../stores/cart'
import api from '../api'

const cart = useCartStore()
const items = computed(()=>cart.items)

onMounted(async () => {
  try {
    const r = await api.get('/api/cart')
    // server returns { cartId, items } or full Cart
    if (r.data) {
      const data = r.data.items ? r.data.items : r.data.items || r.data.items === undefined ? r.data : r.data
      // normalize items to { productId, quantity }
      if (Array.isArray(data)) {
        const mapped = data.map((it:any) => ({ productId: it.productId, quantity: it.quantity }))
        cart.setItems(mapped)
      }
      if (r.data.cartId) localStorage.setItem('cartId', r.data.cartId)
    }
  } catch (e) {
    console.warn('failed to load server cart', e)
  }
})

async function checkout(){
  try{
    await api.post('/api/orders/checkout', { paymentMethod: 'mock' })
    cart.clear()
    localStorage.removeItem('cartId')
    alert('下单成功')
  }catch(e){
    console.error(e)
    alert('下单失败')
  }
}
</script>