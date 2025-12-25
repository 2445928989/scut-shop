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
          <p class="stock">库存：<span :class="{ 'no-stock': product?.stock === 0, 'low-stock': product?.stock! < 10 && product?.stock! > 0 }">{{product?.stock}}</span>件</p>
          <el-button type="primary" size="large" @click="addToCart" :disabled="product?.stock === 0">{{ product?.stock === 0 ? '已售罄' : '加入购物车' }}</el-button>
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
import { ElMessage } from 'element-plus'

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
    const serverItem = r.data && r.data.item
    if (cartId) localStorage.setItem('cartId', cartId)
    cart.add(product.value, 1, serverItem?.id)
    ElMessage.success('已加入购物车')
  } catch (e) {
    console.warn('addToCart (detail): server add failed, using local store', e)
    cart.add(product.value, 1)
    ElMessage.success('已加入本地购物车')
  }
}
</script>

<style scoped>
.product-detail {
  padding: 20px;
}

.media {
  width: 100%;
  height: 400px;
  overflow: hidden;
  border-radius: 4px;
  margin-bottom: 12px;
  transition: transform 0.3s ease;
}

.media:hover {
  transform: scale(1.02);
}

.media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.detail-content {
  padding: 20px;
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 16px;
}

.price {
  font-size: 32px;
  color: #e6a23c;
  font-weight: bold;
  margin: 0 0 12px;
}

.desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 20px;
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.stock {
  font-size: 16px;
  color: #909399;
  margin: 16px 0;
}

.no-stock {
  color: #f56c6c;
  font-weight: bold;
}

.low-stock {
  color: #e6a23c;
  font-weight: bold;
}
</style>