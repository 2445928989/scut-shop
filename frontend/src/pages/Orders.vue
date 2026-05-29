<template>
  <div class="orders-container">
    <h3>我的订单</h3>
    <el-empty v-if="orders.length === 0 && !loading" description="暂无订单记录" />
    
    <div v-loading="loading">
      <el-card v-for="order in orders" :key="order.id" class="order-card">
        <template #header>
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ formatDate(order.createdAt) }}</span>
            <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
          </div>
        </template>
        
        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <div class="item-info">
              <span class="item-name">{{ item.productName }}</span>
              <span class="item-price">¥{{ item.price }} x {{ item.quantity }}</span>
            </div>
            <div class="item-subtotal">¥{{ item.subtotal }}</div>
          </div>
        </div>
        
        <div class="order-footer">
          <div class="order-actions" v-if="order.status===0 || order.status===2">
            <el-button v-if="order.status===0" type="danger" size="small" @click="cancelOrder(order.id)">取消订单</el-button>
            <el-button v-if="order.status===2" type="success" size="small" @click="confirmOrder(order.id)">确认收货</el-button>
          </div>
          <div class="total-amount">
            总计：<span class="amount">¥{{ order.totalAmount }}</span>
          </div>
        </div>
      </el-card>
      
      <div class="pagination" v-if="total > size">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          layout="prev, pager, next"
          :total="total"
          @current-change="fetchOrders"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

onMounted(() => {
  fetchOrders()
})

async function fetchOrders() {
  loading.value = true
  try {
    const r = await api.get('/api/orders', {
      params: { page: page.value, size: size.value }
    })
    orders.value = r.data.items
    total.value = r.data.total
  } catch (e) {
    console.error('Failed to fetch orders', e)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

function getStatusText(status: number) {
  const map: Record<number, string> = {
    0: '已创建',
    1: '已支付',
    2: '已发货',
    3: '已送达',
    4: '已取消',
    5: '已退款'
  }
  return map[status] || '未知状态'
}

function getStatusType(status: number) {
  const map: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'success',
    4: 'danger',
    5: 'info'
  }
  return map[status] || ''
}

async function cancelOrder(id: number) {
  try {
    await ElMessageBox.confirm('确认取消该订单?', '提示', { type: 'warning' })
    await api.post(`/api/orders/${id}/cancel`)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {}
}

async function confirmOrder(id: number) {
  try {
    await ElMessageBox.confirm('确认已收到商品?', '提示', { type: 'warning' })
    await api.post(`/api/orders/${id}/confirm`)
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch {}
}
</script>

<style scoped>
.orders-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.order-card {
  margin-bottom: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.order-no {
  font-weight: bold;
  color: var(--primary);
}

.order-time {
  color: var(--muted);
}

.order-items {
  padding: 10px 0;
}

.order-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.order-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  flex-direction: column;
}

.item-name {
  font-weight: 500;
}

.item-price {
  font-size: 12px;
  color: var(--muted);
}

.item-subtotal {
  font-weight: bold;
}

.order-footer {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.total-amount {
  font-size: 16px;
}

.amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
