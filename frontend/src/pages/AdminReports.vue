<template>
  <div class="admin-reports app-container">
    <div class="header-actions">
      <h2>销售统计报表</h2>
      <el-button :icon="Refresh" @click="fetchStats">刷新</el-button>
    </div>

    <el-row :gutter="20" class="summary-cards">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>总销售额</template>
          <div class="stat-value">¥{{ stats.totalSales || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>总订单数</template>
          <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>每日销售趋势 (最近7天)</template>
          <el-table :data="stats.dailySales" style="width: 100%">
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="amount" label="销售额">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>热销商品排行</template>
          <el-table :data="stats.topProducts" style="width: 100%">
            <el-table-column prop="name" label="商品名称" />
            <el-table-column prop="value" label="销量" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const stats = ref({
  totalSales: 0,
  totalOrders: 0,
  dailySales: [],
  topProducts: []
})

const fetchStats = async () => {
  try {
    const r = await api.get('/api/orders/admin/statistics')
    stats.value = r.data
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-reports { padding: 20px; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.summary-cards .stat-value { font-size: 28px; font-weight: bold; color: #409EFF; text-align: center; padding: 10px 0; }
</style>
