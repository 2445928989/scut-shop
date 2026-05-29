<template>
  <div class="page-container">
    <h2>地址管理</h2>
    <el-button type="primary" @click="openDialog()" style="margin-bottom:16px">添加地址</el-button>
    <el-table :data="addresses" v-loading="loading" stripe>
      <el-table-column prop="recipient" label="收件人" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="地区">
        <template #default="{row}">{{ row.province || '' }}{{ row.city || '' }}{{ row.district || '' }}</template>
      </el-table-column>
      <el-table-column prop="detail" label="详细地址" />
      <el-table-column label="默认" width="80">
        <template #default="{row}">
          <el-tag v-if="row.isDefault===1" type="success" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="editing?.id ? '编辑地址' : '添加地址'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收件人"><el-input v-model="form.recipient" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区/县"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" type="textarea" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefaultBool" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'

const addresses = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<any>(null)
const form = ref({ recipient:'', phone:'', province:'', city:'', district:'', detail:'', isDefaultBool: false })

function openDialog(addr?: any) {
  editing.value = addr || null
  if (addr) {
    form.value = { ...addr, isDefaultBool: addr.isDefault === 1 }
  } else {
    form.value = { recipient:'', phone:'', province:'', city:'', district:'', detail:'', isDefaultBool: false }
  }
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const r = await api.get('/api/addresses')
    addresses.value = r.data
  } catch (e: any) {
    ElMessage.error('加载地址失败')
  } finally { loading.value = false }
}

async function save() {
  const payload = {
    ...form.value,
    isDefault: form.value.isDefaultBool ? 1 : 0
  }
  try {
    if (editing.value?.id) {
      await api.put(`/api/addresses/${editing.value.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await api.post('/api/addresses', payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '操作失败')
  }
}

async function del(id: number) {
  try {
    await ElMessageBox.confirm('确认删除?', '提示', { type: 'warning' })
    await api.delete(`/api/addresses/${id}`)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>
