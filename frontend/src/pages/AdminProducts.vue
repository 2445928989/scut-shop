<template>
  <div>
    <h3>商品管理</h3>
    <el-button type="primary" @click="openCreate" style="margin-bottom:12px">新建商品</el-button>

    <el-table :data="products" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="price" label="价格" width="120"/>
      <el-table-column prop="stock" label="库存" width="100"/>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <span v-if="row.status === 1">上架</span>
          <span v-else>已下架</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" v-if="row.status === 1" type="danger" @click="del(row.id)">下架</el-button>
          <el-button size="small" v-else type="primary" @click="restore(row.id)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="editId ? '编辑商品' : '新建商品'" :visible.sync="showDialog">
      <el-form :model="form">
        <el-form-item label="名称"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="SKU"><el-input v-model="form.sku"/></el-form-item>
        <el-form-item label="价格"><el-input v-model.number="form.price" type="number"/></el-form-item>
        <el-form-item label="库存"><el-input v-model.number="form.stock" type="number"/></el-form-item>
        <el-form-item label="图片链接"><el-input v-model="form.imageUrl"/></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="form.description"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'

const products = ref<any[]>([])
const showDialog = ref(false)
const editId = ref<number | null>(null)
const form = ref<any>({ name: '', sku: '', price: 0, stock: 0, imageUrl: '', description: '', status: 1, categoryId: null })

async function load() {
  // admin wants to see all products including inactive: status=-1 means all
  const r = await api.get('/api/products?status=-1')
  products.value = r.data && r.data.items ? r.data.items : r.data
}

function openCreate() {
  editId.value = null
  form.value = { name: '', sku: '', price: 0, stock: 0, imageUrl: '', description: '', status: 1, categoryId: null }
  showDialog.value = true
}

function openEdit(row:any) {
  editId.value = row.id
  form.value = { ...row }
  showDialog.value = true
}

async function save() {
  if (editId.value) {
    await api.put(`/api/admin/products/${editId.value}`, form.value)
    showDialog.value = false
    await load()
  } else {
    const r = await api.post('/api/admin/products', form.value)
    if (r.data && r.data.id) {
      showDialog.value = false
      await load()
    }
  }
}

import { ElMessage } from 'element-plus'

async function del(id:number) {
  if (!confirm('确认下架该商品？')) return
  try {
    const r = await api.delete(`/api/admin/products/${id}`)
    ElMessage.success(r.data && r.data.status === 'deleted' ? '下架成功' : '下架成功')
  } catch (e:any) {
    const msg = e && e.response && e.response.data && e.response.data.error ? e.response.data.error : '下架失败'
    ElMessage.error(msg)
  }
  await load()
}

async function restore(id:number) {
  if (!confirm('确认恢复该商品？')) return
  try {
    const r = await api.put(`/api/admin/products/${id}`, { status: 1 })
    ElMessage.success('恢复成功')
  } catch (e:any) {
    const msg = e && e.response && e.response.data && e.response.data.error ? e.response.data.error : '恢复失败'
    ElMessage.error(msg)
  }
  await load()
}

onMounted(load)
</script>
