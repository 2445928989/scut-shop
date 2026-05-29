<template>
  <div class="page-container">
    <h2>商品管理 (Sales)</h2>
    <el-table :data="products" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status===1 ? 'success' : 'info'" size="small">{{ row.status===1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button size="small" @click="edit(row)">修改</el-button>
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

    <el-dialog title="修改商品" v-model="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :value="1" label="上架" />
            <el-option :value="0" label="下架" />
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

const products = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const form = ref({ id: 0, price: 0, stock: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const r = await api.get('/api/products', { params: { page: page.value, size: size.value, status: -1 } })
    products.value = r.data.items
    total.value = r.data.total
  } finally { loading.value = false }
}

function edit(row: any) {
  form.value = { id: row.id, price: row.price, stock: row.stock, status: row.status }
  dialogVisible.value = true
}

async function save() {
  try {
    await api.put(`/api/sales/products/${form.value.id}`, {
      price: form.value.price,
      stock: form.value.stock,
      status: form.value.status
    })
    ElMessage.success('更新成功')
    dialogVisible.value = false
    load()
  } catch { ElMessage.error('更新失败') }
}

onMounted(load)
</script>
