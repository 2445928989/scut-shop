<template>
  <div>
    <h3>商品列表</h3>
    <!-- 临时调试面板：显示原始后端响应（开发时用，后来会移除） -->
    <pre v-if="debug" class="debug">{{ JSON.stringify(debug, null, 2) }}</pre>
    <el-row :gutter="20">
      <el-col v-for="p in products" :key="p.id" :span="6">
        <el-card class="product-card">
          <router-link :to="`/product/${p.id}`" class="card-link">
            <div class="media">
              <img v-if="p.imageUrl" :src="p.imageUrl" alt=""/>
              <div v-else class="placeholder">暂无图片</div>
            </div>
            <div class="content">
              <h4 class="title">{{p.name}}</h4>
              <p class="price">¥{{p.price}}</p>
              <p class="excerpt">{{ p.description || '暂无描述' }}</p>
            </div>
          </router-link>
          <div class="actions">
            <router-link :to="`/product/${p.id}`" class="view-link">查看</router-link>
            <el-button type="primary" @click.stop="addToCart(p.id)">加入购物车</el-button>
          </div>
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
const debug = ref<any>(null)

onMounted(async()=>{
  try {
    const r = await api.get('/api/products')
    products.value = r.data && r.data.items ? r.data.items : r.data
    debug.value = r.data
    console.log('loaded products', products.value)
  } catch (e:any) {
    console.error('products fetch failed', e)
    debug.value = { error: String(e), response: e && e.response ? e.response.data : null }
  }
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