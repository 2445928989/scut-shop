<template>
  <div class="admin-orders app-container">
    <div class="header-actions">
      <h2>订单管理</h2>
      <el-button :icon="Refresh" @click="fetchOrders">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="orders" style="width: 100%" class="order-table">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="order-details">
            <h4>订单详情 (ID: {{ row.id }})</h4>
            <el-table :data="row.items" size="small" border>
              <el-table-column prop="productName" label="商品名称" />
              <el-table-column prop="price" label="单价" width="100">
                <template #default="scope">¥{{ scope.row.price }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="subtotal" label="小计" width="100">
                <template #default="scope">¥{{ scope.row.subtotal }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="totalAmount" label="总金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      
      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="下单时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="editStatus(row)">修改状态</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="fetchOrders"
      />
    </div>

    <!-- 修改状态对话框 -->
    <el-dialog v-model="dialogVisible" title="修改订单状态" width="400px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="订单状态">
          <el-select v-model="editForm.status" placeholder="请选择">
            <el-option label="待付款" :value="0" />
            <el-option label="已付款/待发货" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatus" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const orders = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const submitting = ref(false)
const editForm = reactive({
  id: null,
  status: 0,
  paymentStatus: 0
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const r = await api.get('/api/orders/admin/all', {
      params: { page: currentPage.value, size: pageSize.value }
    })
    orders.value = r.data.items
    total.value = r.data.total
  } catch (e) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const editStatus = (row: any) => {
  editForm.id = row.id
  editForm.status = row.status
  editForm.paymentStatus = row.paymentStatus
  dialogVisible.value = true
}

const submitStatus = async () => {
  submitting.value = true
  try {
    await api.put(`/api/orders/admin/${editForm.id}/status`, {
      status: editForm.status,
      paymentStatus: editForm.paymentStatus
    })
    ElMessage.success('状态更新成功')
    dialogVisible.value = false
    fetchOrders()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    submitting.value = false
  }
}

const getStatusText = (status: number) => {
  const map: any = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const getStatusType = (status: number) => {
  const map: any = { 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(fetchOrders)
</script>

<style scoped>
.admin-orders { padding: 20px; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.order-details { padding: 10px 20px; background: #f9f9f9; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
