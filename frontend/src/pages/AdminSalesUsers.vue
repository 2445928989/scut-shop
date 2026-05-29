<template>
  <div class="page-container">
    <h2>销售人员管理</h2>
    <el-button type="primary" @click="openCreate" style="margin-bottom:16px">添加销售人员</el-button>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status===1 ? 'success' : 'danger'" size="small">{{ row.status===1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{row}">
          <el-button size="small" @click="openResetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="disableUser(row)">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="添加销售人员" v-model="createVisible" width="400px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="createForm.username" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="createForm.email" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="createForm.password" type="password" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog title="重置密码" v-model="resetPwdVisible" width="350px">
      <el-form label-width="80px">
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible=false">取消</el-button>
        <el-button type="primary" @click="doResetPwd">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref<any[]>([])
const loading = ref(false)
const createVisible = ref(false)
const createForm = ref({ username: '', email: '', password: '' })
const resetPwdVisible = ref(false)
const resetUserId = ref(0)
const newPassword = ref('')

async function load() {
  loading.value = true
  try {
    const r = await api.get('/api/admin/sales')
    users.value = r.data
  } finally { loading.value = false }
}

function openCreate() {
  createForm.value = { username: '', email: '', password: '' }
  createVisible.value = true
}

async function doCreate() {
  try {
    await api.post('/api/admin/sales', createForm.value)
    ElMessage.success('创建成功')
    createVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '创建失败')
  }
}

function openResetPwd(row: any) {
  resetUserId.value = row.id
  newPassword.value = ''
  resetPwdVisible.value = true
}

async function doResetPwd() {
  try {
    await api.put(`/api/admin/sales/${resetUserId.value}/reset-password`, { password: newPassword.value })
    ElMessage.success('密码已重置')
    resetPwdVisible.value = false
  } catch { ElMessage.error('操作失败') }
}

async function disableUser(row: any) {
  try {
    await ElMessageBox.confirm(`确认禁用用户 "${row.username}"?`, '提示', { type: 'warning' })
    await api.delete(`/api/admin/sales/${row.id}`)
    ElMessage.success('已禁用')
    load()
  } catch {}
}

onMounted(load)
</script>
