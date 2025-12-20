<template>
  <div class="product-detail">
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="media">
          <img v-if="product?.imageUrl" :src="product.imageUrl" alt="" />
          <div v-else class="placeholder">暂无图片</div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="detail-content">
          <h3 class="title">{{product?.name}}</h3>
          <p class="price">¥{{product?.price}}</p>
          <p class="desc">{{product?.description || '暂无描述'}}</p>
          <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const product = ref<any|null>(null)
const cart = useCartStore()

onMounted(async ()=>{
  const r = await api.get(`/api/products/${route.params.id}`)
  product.value = r.data
})

async function addToCart(){
  if (!product.value) return
  // try to create / update server cart and persist cartId
  try {
    const r = await api.post('/api/cart/items', { productId: product.value.id, quantity: 1 })
    const cartId = r.data && r.data.cartId
    if (cartId) localStorage.setItem('cartId', cartId)
  } catch (e) {
    console.warn('addToCart (detail): server add failed, using local store', e)
  }
  cart.add(product.value.id, 1)
  alert('已加入购物车')
}
</script>