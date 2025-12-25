<template>
  <div>
    <h3>商品列表</h3>
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
              <p class="stock">库存：<span :class="{ 'low-stock': p.stock < 10 }">{{p.stock}}</span>件</p>
            </div>
          </router-link>
          <div class="actions">
            <router-link :to="`/product/${p.id}`" class="view-link">查看</router-link>
            <el-button type="primary" @click.stop="addToCart(p)">加入购物车</el-button>
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
import { ElMessage } from 'element-plus'

const products = ref<any[]>([])
const cart = useCartStore()

onMounted(async()=>{
  try {
    // 主页只显示已上架的商品（status=1）
    const r = await api.get('/api/products', { params: { status: 1 } })
    products.value = r.data && r.data.items ? r.data.items : r.data
  } catch (e:any) {
    console.error('products fetch failed', e)
  }
})

async function addToCart(product: any) {
  try {
    const r = await api.post('/api/cart/items', { productId: product.id, quantity: 1 })
    const cartId = r.data && r.data.cartId
    const serverItem = r.data && r.data.item
    if (cartId) localStorage.setItem('cartId', cartId)
    cart.add(product, 1, serverItem?.id)
    ElMessage.success('已加入购物车')
  } catch (e) {
    console.warn('addToCart failed', e)
    // Fallback to local cart if server fails
    cart.add(product, 1)
    ElMessage.success('已加入本地购物车')
  }
}
</script>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: all 0.3s ease;
}

:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-4px);
}

.card-link {
  text-decoration: none;
  color: inherit;
  flex: 1;
}

.media {
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 4px;
  margin-bottom: 12px;
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

.content {
  flex: 1;
}

.title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  font-size: 20px;
  color: #e6a23c;
  font-weight: bold;
  margin: 4px 0;
}

.excerpt {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin: 4px 0;
}

.stock {
  font-size: 12px;
  color: #909399;
  margin: 8px 0;
}

.low-stock {
  color: #e6a23c;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: space-between;
  flex-wrap: wrap;
  width: 100%;
}

.view-link {
  flex: 1 1 48%;
  text-align: center;
  padding: 8px;
  background: #f0f9ff;
  border-radius: 4px;
  transition: all 0.3s ease;
  text-decoration: none;
  color: #409eff;
  font-weight: 500;
}

.view-link:hover {
  background: #409eff;
  color: white;
  transform: translateY(-2px);
}

.actions :deep(.el-button) {
  flex: 1 1 48%;
  transition: all 0.3s ease;
}

.actions :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.view-link {
  flex: 1;
}

.actions :deep(.el-button) {
  flex: 1;
}
</style>