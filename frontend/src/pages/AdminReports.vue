<template>
  <div class="admin-reports app-container">
    <div class="header-actions">
      <h2>销售统计报表</h2>
      <div>
        <el-radio-group v-model="timeRange" size="small" @change="fetchStats" style="margin-right:12px">
          <el-radio-button value="7">7天</el-radio-button>
          <el-radio-button value="30">30天</el-radio-button>
          <el-radio-button value="90">90天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" @click="fetchStats">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="20" class="summary-cards">
      <el-col :span="6">
        <el-card shadow="hover"><template #header>总销售额</template>
          <div class="stat-value">¥{{ formatNumber(stats.totalSales || 0) }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><template #header>总订单数</template>
          <div class="stat-value">{{ stats.totalOrders || 0 }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><template #header>日均销售额</template>
          <div class="stat-value">¥{{ formatNumber(avgDaily) }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><template #header>热销商品数</template>
          <div class="stat-value">{{ (stats.topProducts || []).length }}</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>销售趋势</span>
            <el-radio-group v-model="chartType" size="small" style="margin-left:12px" @change="fetchStats">
              <el-radio-button value="daily">日</el-radio-button>
              <el-radio-button value="weekly">周</el-radio-button>
              <el-radio-button value="monthly">月</el-radio-button>
            </el-radio-group>
          </template>
          <v-chart :option="trendOption" style="height:350px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>热销商品排行</template>
          <v-chart :option="barOption" style="height:350px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px" v-if="anomalies.length > 0">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span style="color:#f56c6c">销售异常告警</span></template>
          <el-table :data="anomalies" size="small">
            <el-table-column prop="date" label="日期" width="150" />
            <el-table-column label="实际销售额"><template #default="{row}">¥{{ row.amount }}</template></el-table-column>
            <el-table-column label="预期均值"><template #default="{row}">¥{{ row.expected }}</template></el-table-column>
            <el-table-column label="异常阈值"><template #default="{row}">¥{{ row.threshold }}</template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref<any>({})
const anomalies = ref<any[]>([])
const forecast = ref<any[]>([])
const timeRange = ref('30')
const chartType = ref('daily')

const avgDaily = computed(() => {
  const ds = stats.value.dailySales
  if (!ds || ds.length === 0) return 0
  const sum = ds.reduce((s: number, r: any) => s + (Number(r.amount) || 0), 0)
  return Math.round(sum / ds.length * 100) / 100
})

const trendOption = computed(() => {
  const data = chartType.value === 'weekly' ? stats.value.weeklySales
    : chartType.value === 'monthly' ? stats.value.monthlySales
    : stats.value.dailySales
  if (!data || data.length === 0) return {}

  const dates = data.map((r: any) => r.date || r.week || r.month)
  const actualValues = data.map((r: any) => Number(r.amount) || 0)

  // 如果有预测数据，扩展 x 轴和 series
  const forecastData = forecast.value || []
  const allDates = forecastData.length > 0
    ? [...dates, ...forecastData.map((_: any, i: number) => `预测+${i + 1}天`)]
    : dates

  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: allDates },
    yAxis: { type: 'value', name: '销售额 (¥)' },
    series: [{
      name: '实际销售额', type: 'line',
      data: [
        ...actualValues,
        ...forecastData.map(() => null)
      ],
      smooth: true, areaStyle: { opacity: 0.1 }
    },
    ...(forecastData.length > 0 ? [{
      name: '预测趋势', type: 'line',
      data: [
        ...actualValues.map(() => null),
        ...forecastData.map((f: any) => Math.round(f.amount * 100) / 100)
      ],
      lineStyle: { type: 'dashed', color: '#e6a23c' },
      itemStyle: { color: '#e6a23c' },
      smooth: true
    }] : [])]
  }
})

const barOption = computed(() => {
  const data = stats.value.topProducts || []
  if (data.length === 0) return {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map((r: any) => r.name).reverse(),
      axisLabel: { width: 100, overflow: 'truncate' } },
    series: [{ type: 'bar', data: data.map((r: any) => r.value).reverse(),
      itemStyle: { color: '#409EFF' } }]
  }
})

function formatNumber(n: any) {
  const num = Number(n)
  if (!num) return '0'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchStats() {
  try {
    const [statResp, anomalyResp, forecastResp] = await Promise.all([
      api.get('/api/analytics/sales-stats', { params: { range: timeRange.value } }),
      api.get('/api/analytics/anomalies', { params: { days: timeRange.value } }).catch(() => ({ data: [] })),
      api.get('/api/analytics/forecast', { params: { days: parseInt(timeRange.value), forecast: 7 } }).catch(() => ({ data: [] }))
    ])
    stats.value = statResp.data
    anomalies.value = anomalyResp.data || []
    forecast.value = forecastResp.data || []
  } catch (e) {
    ElMessage.error('获取统计数据失败')
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-reports { padding: 20px; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.summary-cards .stat-value { font-size: 24px; font-weight: bold; color: #409EFF; text-align: center; padding: 8px 0; }
</style>
