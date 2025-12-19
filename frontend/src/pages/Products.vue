<template>
  <div>
    <h3>商品列表</h3>
    <el-row :gutter="20">
      <el-col v-for="p in products" :key="p.id" :span="6">
        <el-card>
          <h4>{{p.name}}</h4>
          <p>¥{{p.price}}</p>
          <router-link :to="`/product/${p.id}`">查看</router-link>
          <el-button type="primary" @click="addToCart(p.id)">加入购物车</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { useCartStore } from '../stores/cart'

const products = ref<any[]>([])
const cart = useCartStore()

onMounted(async()=>{
  const r = await api.get('/api/products')
  products.value = r.data && r.data.items ? r.data.items : r.data
  console.log('loaded products', products.value)
})

async function addToCart(id:number){
  console.log('addToCart called', id)
  try {
    const r = await api.post('/api/cart/items', { productId: id, quantity: 1 })
    const cartId = r.data && r.data.cartId
    if (cartId) localStorage.setItem('cartId', cartId)
  } catch (e) {
    // ignore network errors; fall back to client-side cart
    console.warn('addToCart: server add failed, using local store', e)
  }
  cart.add(id,1)
  alert('已加入购物车')
}
</script>