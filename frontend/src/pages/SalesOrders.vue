<template>
  <div class="page-container">
    <h2>订单管理 (Sales)</h2>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="280" />
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column label="订单状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.paymentStatus===1 ? 'success' : 'warning'" size="small">{{ row.paymentStatus===1 ? '已支付' : '未支付' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button size="small" @click="openStatus(row)">改状态</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="prev, pager, next"
      @current-change="load"
      style="margin-top:16px; justify-content: center;" />

    <el-dialog title="修改订单状态" v-model="dialogVisible" width="350px">
      <el-form label-width="80px">
        <el-form-item label="订单状态">
          <el-select v-model="form.status">
            <el-option :value="0" label="已创建" />
            <el-option :value="1" label="已支付" />
            <el-option :value="2" label="已发货" />
            <el-option :value="3" label="已送达" />
            <el-option :value="4" label="已取消" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({ id: 0, status: 0, paymentStatus: 0 })

function statusText(s: number) {
  const map: Record<number,string> = { 0:'已创建',1:'已支付',2:'已发货',3:'已送达',4:'已取消',5:'已退款' }
  return map[s] || '未知'
}
function statusType(s: number) {
  if (s===1||s===2) return 'primary'; if (s===3) return 'success'; if (s===4||s===5) return 'info'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const r = await api.get('/api/sales/orders', { params: { page: page.value, size: size.value } })
    orders.value = r.data.items
    total.value = r.data.total
  } finally { loading.value = false }
}

function openStatus(row: any) {
  form.value = { id: row.id, status: row.status, paymentStatus: row.paymentStatus }
  dialogVisible.value = true
}

async function save() {
  try {
    await api.put(`/api/orders/admin/${form.value.id}/status`, { status: form.value.status, paymentStatus: form.value.paymentStatus })
    ElMessage.success('更新成功')
    dialogVisible.value = false
    load()
  } catch { ElMessage.error('更新失败') }
}

onMounted(load)
</script>
