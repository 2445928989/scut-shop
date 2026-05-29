<template>
  <div class="dashboard">
    <!-- 顶部标题栏 -->
    <div class="dash-header">
      <h1>SCUT-Shop 实时数据大屏</h1>
      <div class="header-right">
        <span class="clock">{{ now }}</span>
        <el-tag :type="refreshCount > 0 ? 'success' : 'info'" size="small">自动刷新 30s</el-tag>
        <el-button type="primary" size="small" @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '全屏' }}</el-button>
      </div>
    </div>

    <!-- 核心指标 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6">
        <div class="kpi-card kpi-blue">
          <div class="kpi-label">总销售额</div>
          <div class="kpi-value">¥{{ fmt(stats.totalSales) }}</div>
          <div class="kpi-sub">累计</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-green">
          <div class="kpi-label">总订单数</div>
          <div class="kpi-value">{{ stats.totalOrders }}</div>
          <div class="kpi-sub">累计</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-orange">
          <div class="kpi-label">日均销售额</div>
          <div class="kpi-value">¥{{ fmt(avgDaily) }}</div>
          <div class="kpi-sub">近30天</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card kpi-red">
          <div class="kpi-label">异常天数</div>
          <div class="kpi-value">{{ anomalies.length }}<span style="font-size:18px">天</span></div>
          <div class="kpi-sub">近30天</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <div class="chart-box">
          <div class="chart-title">销售趋势 & 预测</div>
          <v-chart :option="trendOption" autoresize style="height:340px" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">热销商品 TOP8</div>
          <v-chart :option="barOption" autoresize style="height:340px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">用户地域分布</div>
          <v-chart :option="regionOption" autoresize style="height:280px" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">销售异常监控</div>
          <div v-if="anomalies.length === 0" class="no-data">近期无异常</div>
          <div v-else class="anomaly-list">
            <div v-for="a in anomalies.slice(0,6)" :key="a.date" class="anomaly-item">
              <span class="an-date">{{ a.date }}</span>
              <span class="an-val">¥{{ fmt(a.amount) }}</span>
              <span class="an-expect">预期 ¥{{ fmt(a.expected) }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-box">
          <div class="chart-title">未来预测</div>
          <div class="forecast-list">
            <div v-for="f in forecast" :key="f.day" class="forecast-item">
              <span class="fc-day">预测+{{ f.day }}天</span>
              <span class="fc-val">¥{{ fmt(f.amount) }}</span>
              <div class="fc-bar"><div :style="{width: Math.min(100, f.amount / maxForecast * 100) + '%'}"></div></div>
            </div>
            <div v-if="forecast.length === 0" class="no-data">加载中...</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="dash-footer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const stats = ref<any>({})
const anomalies = ref<any[]>([])
const forecast = ref<any[]>([])
const now = ref('')
const refreshCount = ref(0)
const isFullscreen = ref(false)
let timer: any = null

function fmt(v: any) {
  const n = Number(v) || 0
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
}

const avgDaily = computed(() => {
  const ds = stats.value.dailySales
  if (!ds?.length) return 0
  return ds.reduce((s: number, r: any) => s + (Number(r.amount) || 0), 0) / ds.length
})

const maxForecast = computed(() => Math.max(...forecast.value.map((f: any) => f.amount || 0), 1))

const trendOption = computed(() => {
  const data = stats.value.dailySales || []
  if (!data.length) return {}
  const dates = data.map((r: any) => r.date)
  const values = data.map((r: any) => Number(r.amount) || 0)
  const fd = forecast.value || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: [...dates, ...fd.map((_: any, i: number) => `+${i + 1}天`)], axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '¥', axisLabel: { formatter: (v: number) => (v / 10000).toFixed(0) + 'w' } },
    series: [
      { name: '实际', type: 'line', data: [...values, ...fd.map(() => null)], smooth: true, areaStyle: { opacity: 0.08 }, lineStyle: { width: 2 } },
      ...(fd.length ? [{ name: '预测', type: 'line', data: [...values.map(() => null), ...fd.map((f: any) => Math.round(f.amount * 100) / 100)], lineStyle: { type: 'dashed', width: 2, color: '#e6a23c' }, itemStyle: { color: '#e6a23c' }, smooth: true }] : [])
    ]
  }
})

const barOption = computed(() => {
  const data = (stats.value.topProducts || []).slice(0, 8)
  if (!data.length) return {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 5, right: 20, bottom: 5, left: 0, containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: { type: 'category', data: data.map((r: any) => r.name).reverse(), axisLabel: { fontSize: 11, width: 100, overflow: 'truncate' }, inverse: true },
    series: [{ type: 'bar', data: data.map((r: any) => r.value).reverse(), itemStyle: { borderRadius: [0, 4, 4, 0], color: '#409EFF' } }]
  }
})

const regionOption = computed(() => {
  const ds = stats.value.dailySales || []
  if (!ds.length) return {}
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['50%', '55%'],
      label: { fontSize: 12, formatter: '{b}\n{d}%' },
      data: [
        { value: 35, name: '广东' }, { value: 25, name: '北京' },
        { value: 18, name: '上海' }, { value: 12, name: '浙江' }, { value: 10, name: '江苏' }
      ],
      emphasis: { label: { fontSize: 16, fontWeight: 'bold' } }
    }]
  }
})

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function exitFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen()
}

function updateClock() { now.value = new Date().toLocaleString('zh-CN') }

async function fetchData() {
  try {
    const [statR, anoR, foreR] = await Promise.all([
      api.get('/api/analytics/sales-stats', { params: { range: 30 } }),
      api.get('/api/analytics/anomalies', { params: { days: 30 } }).catch(() => ({ data: [] })),
      api.get('/api/analytics/forecast', { params: { days: 30, forecast: 7 } }).catch(() => ({ data: [] }))
    ])
    stats.value = statR.data
    anomalies.value = anoR.data || []
    forecast.value = foreR.data || []
    refreshCount.value++
  } catch {}
}

onMounted(() => {
  updateClock()
  setInterval(updateClock, 1000)
  fetchData()
  timer = setInterval(fetchData, 30000)
  document.addEventListener('fullscreenchange', () => { isFullscreen.value = !!document.fullscreenElement })
  document.addEventListener('keydown', (e) => { if (e.key === 'f' && e.ctrlKey) { e.preventDefault(); toggleFullscreen() } })
})
onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.dashboard { background: #0a1628; min-height: 100vh; padding: 16px 24px; color: #e0e6ed; font-family: 'Microsoft YaHei', sans-serif; }
.dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,.1); }
.dash-header h1 { margin: 0; font-size: 26px; background: linear-gradient(90deg, #409EFF, #67C23A); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header-right { display: flex; align-items: center; gap: 16px; }
.clock { font-size: 16px; color: #a0aec0; font-variant-numeric: tabular-nums; }

.kpi-row { margin-bottom: 14px; }
.kpi-card { border-radius: 8px; padding: 16px 20px; position: relative; overflow: hidden; }
.kpi-card::after { content: ''; position: absolute; top: 0; right: 0; width: 80px; height: 80px; border-radius: 50%; opacity: .1; transform: translate(20px, -20px); }
.kpi-blue { background: linear-gradient(135deg, #1a3a5c, #0f2744); }
.kpi-blue::after { background: #409EFF; }
.kpi-green { background: linear-gradient(135deg, #1a3c2e, #0f241b); }
.kpi-green::after { background: #67C23A; }
.kpi-orange { background: linear-gradient(135deg, #3c2e1a, #241b0f); }
.kpi-orange::after { background: #e6a23c; }
.kpi-red { background: linear-gradient(135deg, #3c1a1a, #240f0f); }
.kpi-red::after { background: #f56c6c; }
.kpi-label { font-size: 13px; color: #8899aa; margin-bottom: 6px; }
.kpi-value { font-size: 30px; font-weight: 800; }
.kpi-sub { font-size: 11px; color: #667788; margin-top: 4px; }

.chart-row { margin-bottom: 14px; }
.chart-box { background: rgba(255,255,255,.03); border: 1px solid rgba(255,255,255,.06); border-radius: 8px; padding: 14px; }
.chart-title { font-size: 14px; font-weight: 600; color: #8899aa; margin-bottom: 8px; padding-left: 8px; border-left: 3px solid #409EFF; }

.anomaly-list { max-height: 240px; overflow-y: auto; }
.anomaly-item { display: flex; align-items: center; gap: 12px; padding: 8px 10px; border-bottom: 1px solid rgba(255,255,255,.04); }
.an-date { color: #f56c6c; font-weight: 600; width: 90px; }
.an-val { color: #e0e6ed; font-weight: 600; width: 80px; }
.an-expect { color: #667788; font-size: 12px; }

.forecast-list { max-height: 240px; overflow-y: auto; }
.forecast-item { padding: 6px 10px; border-bottom: 1px solid rgba(255,255,255,.04); }
.fc-day { color: #e6a23c; font-weight: 600; margin-right: 12px; }
.fc-val { color: #e0e6ed; font-weight: 600; }
.fc-bar { height: 4px; background: rgba(255,255,255,.08); border-radius: 2px; margin-top: 4px; }
.fc-bar div { height: 100%; background: linear-gradient(90deg, #e6a23c, #f56c6c); border-radius: 2px; transition: width .5s; }

.no-data { color: #667788; text-align: center; padding: 40px 0; font-size: 14px; }
.dash-footer { text-align: center; padding: 8px; }
.exit-hint { color: #556677; font-size: 12px; }
</style>
