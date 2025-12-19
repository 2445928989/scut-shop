<template>
  <div>
    <h3>{{product?.name}}</h3>
    <p>价格: ¥{{product?.price}}</p>
    <el-button type="primary" @click="addToCart">加入购物车</el-button>
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

function addToCart(){
  if(product.value) cart.add(product.value.id, 1)
  alert('已加入购物车')
}
</script>