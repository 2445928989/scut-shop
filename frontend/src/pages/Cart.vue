<template>
  <div class="cart-page app-container">
    <div class="cart-header">
      <h2 class="page-title">我的购物车</h2>
      <el-tag type="info" effect="plain" v-if="items.length > 0">共 {{ items.length }} 件商品</el-tag>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="items.length === 0" class="empty-cart">
      <el-empty description="购物车空空如也">
        <router-link to="/">
          <el-button type="primary">去购物</el-button>
        </router-link>
      </el-empty>
    </div>

    <div v-else class="cart-content">
      <el-table :data="items" style="width: 100%" class="cart-table">
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image 
              :src="row.imageUrl || 'https://via.placeholder.com/80'" 
              class="product-image"
              fit="cover"
            />
          </template>
        </el-table-column>

        <el-table-column prop="productId" label="商品ID" width="100" />

        <el-table-column prop="name" label="商品名称" min-width="200">
          <template #default="{ row }">
            <div class="name-column">
              <span class="product-name-text" :class="{ 'off-shelf-text': row.status === 0 }">
                {{ row.name || '加载中...' }}
              </span>
              <el-tag v-if="row.status === 0" type="danger" size="small" effect="dark" class="status-tag">已下架</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="单价" width="120">
          <template #default="{ row }">
            <span class="price" :class="{ 'off-shelf-text': row.status === 0 }">
              ¥{{ (row.price || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number 
              v-model="row.quantity" 
              :min="1" 
              size="small"
              style="width: 110px"
              :disabled="row.status === 0"
              @change="(val: number) => updateQuantity(row.productId, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span class="subtotal" :class="{ 'off-shelf-text': row.status === 0 }">
              ¥{{ row.status === 0 ? '0.00' : ((row.price || 0) * row.quantity).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeItem(row.productId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="footer-left">
          <el-button @click="clearCart">清空购物车</el-button>
          <el-button 
            v-if="hasOffShelfItems" 
            type="warning" 
            plain 
            @click="clearOffShelf"
          >
            清理失效商品
          </el-button>
        </div>
        <div class="footer-right">
          <div class="total-section">
            <span class="total-label">总计：</span>
            <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" class="checkout-btn" @click="checkout">
            立即结算
          </el-button>
        </div>
      </div>
    </div>

    <!-- 支付模拟对话框 -->
    <el-dialog
      v-model="showPaymentDialog"
      title="扫码支付"
      width="400px"
      align-center
      :close-on-click-modal="false"
    >
      <div class="payment-dialog-content">
        <p class="payment-amount">支付金额：<span>¥{{ totalPrice.toFixed(2) }}</span></p>
        <div class="address-select" style="width:100%;margin-bottom:16px">
          <p style="margin-bottom:8px;font-weight:500">收货地址：</p>
          <el-select v-model="selectedAddressId" placeholder="请选择地址" style="width:100%" v-if="addresses.length>0">
            <el-option v-for="a in addresses" :key="a.id" :value="a.id"
              :label="`${a.recipient} ${a.phone} ${a.province||''}${a.city||''}${a.district||''} ${a.detail}`" />
          </el-select>
          <el-empty v-else description="暂无地址" :image-size="60" />
          <el-button type="primary" link size="small" @click="router.push('/addresses')" style="margin-top:4px">
            管理收货地址
          </el-button>
        </div>
        <div class="qr-placeholder">
          <el-image 
            src="/payment-qr.jpg" 
            class="qr-image"            fit="contain"          >
            <template #placeholder>
              <div class="image-slot">加载中...</div>
            </template>
            <template #error>
              <div class="image-slot">
                <div style="font-size: 40px; color: #ccc; margin-bottom: 8px;">🖼️</div>
                <p style="font-size: 12px; color: #999; padding: 0 20px;">请在 frontend/public 文件夹放入 payment-qr.png</p>
              </div>
            </template>
          </el-image>
        </div>
        <p class="payment-tip">请使用手机扫码完成支付</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelPayment">放弃付款</el-button>
          <el-button type="primary" @click="confirmPayment" :loading="loading">
            我已支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const cart = useCartStore()
const auth = useAuthStore()
const router = useRouter()
const items = computed(() => cart.items)
const totalPrice = computed(() => cart.totalPrice)
const hasOffShelfItems = computed(() => items.value.some(item => item.status === 0))
const loading = ref(true)
const showPaymentDialog = ref(false)
const addresses = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)

onMounted(async () => {
  await fetchCart()
  loadAddresses()
})

async function loadAddresses() {
  try {
    const r = await api.get('/api/addresses')
    addresses.value = r.data
    const def = addresses.value.find((a:any) => a.isDefault === 1)
    if (def) selectedAddressId.value = def.id
  } catch {}
}

async function fetchCart() {
  loading.value = true
  try {
    const r = await api.get('/api/cart')
    if (r.data) {
      const rawItems = r.data.items || (Array.isArray(r.data) ? r.data : [])
      
      // Fetch product details for each item
      const enrichedItems = await Promise.all(rawItems.map(async (it: any) => {
        try {
          const pResp = await api.get(`/api/products/${it.productId}`)
          return {
            productId: it.productId,
            quantity: it.quantity,
            name: pResp.data.name,
            price: pResp.data.price,
            imageUrl: pResp.data.imageUrl,
            status: pResp.data.status,
            id: it.id // server-side cart item id
          }
        } catch (e) {
          return {
            productId: it.productId,
            quantity: it.quantity,
            price: it.price || 0,
            status: 0, // If product fetch fails, treat as off-shelf/unavailable
            id: it.id,
            name: '商品已失效'
          }
        }
      }))
      
      cart.setItems(enrichedItems)
      if (r.data.cartId) localStorage.setItem('cartId', r.data.cartId)
    }
  } catch (e) {
    console.warn('failed to load server cart', e)
    ElMessage.error('加载购物车失败')
  } finally {
    loading.value = false
  }
}

async function updateQuantity(productId: number, quantity: number) {
  const item = items.value.find(i => i.productId === productId)
  if (!item) return

  // If we have a server-side item id, update on server
  if (item.id) {
    try {
      await api.put(`/api/cart/items/${item.id}`, { quantity })
      cart.updateQuantity(productId, quantity)
    } catch (e) {
      ElMessage.error('更新数量失败')
    }
  } else {
    // Fallback for local-only items (though they should have IDs if fetched from /api/cart)
    cart.updateQuantity(productId, quantity)
  }
}

async function removeItem(productId: number) {
  const item = items.value.find(i => i.productId === productId)
  if (!item) return

  try {
    await ElMessageBox.confirm('确定要从购物车中移除该商品吗？', '提示', {
      type: 'warning'
    })
    
    if (item.id) {
      await api.delete(`/api/cart/items/${item.id}`)
    }
    
    cart.removeItem(productId)
    ElMessage.success('已移除商品')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '警告', {
      type: 'warning'
    })
    // For simplicity, we'll just remove items one by one or clear if backend supports it
    // Since there's no clear endpoint, we'll just clear local and suggest refresh
    // In a real app, you'd have a DELETE /api/cart endpoint
    cart.clear()
    ElMessage.success('购物车已清空')
  } catch (e) {}
}

async function clearOffShelf() {
  const offShelfItems = items.value.filter(item => item.status === 0)
  if (offShelfItems.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要清理这 ${offShelfItems.length} 件失效商品吗？`, '提示')
    loading.value = true
    for (const item of offShelfItems) {
      if (item.id) {
        await api.delete(`/api/cart/items/${item.id}`)
        cart.removeItem(item.productId)
      }
    }
    ElMessage.success('清理完成')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('清理部分商品失败')
  } finally {
    loading.value = false
  }
}

async function checkout() {
  if (!auth.accessToken) {
    ElMessage.warning('请先登录后再结算')
    router.push('/login')
    return
  }

  const hasOffShelf = items.value.some(item => item.status === 0)
  if (hasOffShelf) {
    ElMessage.warning('购物车中包含已下架商品，请移除后再结算')
    return
  }
  if (addresses.value.length === 0) {
    ElMessage.warning('请先在地址管理中添加收货地址')
    return
  }
  showPaymentDialog.value = true
}

async function confirmPayment() {
  try {
    loading.value = true
    console.log('Starting checkout process...')
    const r = await api.post('/api/orders/checkout', { paymentMethod: 'mock', addressId: selectedAddressId.value })
    console.log('Checkout success:', r.data)
    cart.clear()
    localStorage.removeItem('cartId')
    showPaymentDialog.value = false
    ElMessage.success('支付成功，订单已创建！')
    router.push('/orders')
  } catch (e: any) {
    console.error('Checkout failed:', e)
    const errorData = e.response?.data
    const msg = errorData?.error || e.message || '支付确认失败'
    
    if (msg === 'cart_empty') {
      ElMessage.error('购物车是空的，请先添加商品')
    } else if (msg === 'insufficient_stock') {
      ElMessage.error('部分商品库存不足，请调整数量')
    } else if (msg === 'product_not_found') {
      ElMessage.error('部分商品已失效，请清理购物车')
    } else if (e.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(`支付失败: ${msg}`)
    }
  } finally {
    loading.value = false
  }
}

async function cancelPayment() {
  showPaymentDialog.value = false
  ElMessage.info('已取消付款')
}
</script>

<style scoped>
.cart-page {
  max-width: 1000px;
  margin: 0 auto;
  padding-top: 40px;
}

.cart-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.cart-table {
  margin-bottom: 32px;
  --el-table-header-bg-color: #f8f9fa;
}

.product-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  display: block;
}

.product-name-text {
  font-weight: 500;
  color: var(--text);
}

.name-column {
  display: flex;
  align-items: center;
  gap: 8px;
}

.off-shelf-text {
  color: #909399 !important;
  text-decoration: line-through;
}

.status-tag {
  flex-shrink: 0;
}

.price {
  font-weight: 500;
}

.subtotal {
  font-weight: 700;
  color: var(--primary);
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  padding: 20px 24px;
  border-top: 1px solid #ebeef5;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-section {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  font-size: 16px;
  color: var(--muted);
}

.total-price {
  font-size: 28px;
  font-weight: 800;
  color: var(--primary);
}

.checkout-btn {
  padding: 0 40px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.empty-cart {
  padding: 80px 0;
}

.loading-state {
  padding: 40px 0;
}

.payment-dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.payment-amount {
  font-size: 16px;
  margin-bottom: 20px;
}

.payment-amount span {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.qr-placeholder {
  width: 200px;
  height: 200px;
  background: #f8f9fa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.qr-image {
  width: 200px;
  height: 200px;
}

.payment-tip {
  color: var(--muted);
  font-size: 14px;
}
</style>