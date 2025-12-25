<template>
  <div class="admin-users app-container">
    <div class="header-actions">
      <h2>用户管理与日志</h2>
      <el-button :icon="Refresh" @click="fetchUsers">刷新用户</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户列表" name="users">
        <el-table v-loading="loadingUsers" :data="users" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewLogs(row)">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="userPage"
            :page-size="userPageSize"
            layout="total, prev, pager, next"
            :total="userTotal"
            @current-change="fetchUsers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="所有日志" name="logs">
        <div class="log-filter" style="margin-bottom: 10px;">
          <el-input v-model="filterUserId" placeholder="按用户ID筛选" style="width: 200px; margin-right: 10px;" clearable @clear="fetchLogs" />
          <el-button type="primary" @click="fetchLogs">筛选</el-button>
        </div>
        <el-table v-loading="loadingLogs" :data="logs" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="action" label="操作类型" width="150">
            <template #default="{ row }">
              <el-tag :type="getActionType(row.action)">{{ row.action }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="details" label="详情" />
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="logPage"
            :page-size="logPageSize"
            layout="total, prev, pager, next"
            :total="logTotal"
            @current-change="fetchLogs"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 用户日志对话框 -->
    <el-dialog v-model="logDialogVisible" :title="`用户日志: ${selectedUser?.username}`" width="80%">
      <el-table v-loading="loadingUserLogs" :data="userLogs" style="width: 100%">
        <el-table-column prop="action" label="操作类型" width="150">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="details" label="详情" />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table-column>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="userLogPage"
          :page-size="userLogPageSize"
          layout="total, prev, pager, next"
          :total="userLogTotal"
          @current-change="fetchUserLogs"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const activeTab = ref('users')

// Users
const users = ref([])
const loadingUsers = ref(false)
const userTotal = ref(0)
const userPage = ref(1)
const userPageSize = ref(10)

// Logs
const logs = ref([])
const loadingLogs = ref(false)
const logTotal = ref(0)
const logPage = ref(1)
const logPageSize = ref(20)
const filterUserId = ref('')

// User Specific Logs
const logDialogVisible = ref(false)
const selectedUser = ref<any>(null)
const userLogs = ref([])
const loadingUserLogs = ref(false)
const userLogTotal = ref(0)
const userLogPage = ref(1)
const userLogPageSize = ref(10)

const fetchUsers = async () => {
  loadingUsers.value = true
  try {
    const r = await api.get('/api/admin/users', {
      params: { page: userPage.value, size: userPageSize.value }
    })
    users.value = r.data.items
    userTotal.value = r.data.total
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

const fetchLogs = async () => {
  loadingLogs.value = true
  try {
    const r = await api.get('/api/admin/users/logs', {
      params: { 
        page: logPage.value, 
        size: logPageSize.value,
        userId: filterUserId.value || undefined
      }
    })
    logs.value = r.data.items
    logTotal.value = r.data.total
  } catch (e) {
    ElMessage.error('获取日志失败')
  } finally {
    loadingLogs.value = false
  }
}

const viewLogs = (user: any) => {
  selectedUser.value = user
  userLogPage.value = 1
  logDialogVisible.value = true
  fetchUserLogs()
}

const fetchUserLogs = async () => {
  if (!selectedUser.value) return
  loadingUserLogs.value = true
  try {
    const r = await api.get('/api/admin/users/logs', {
      params: { 
        userId: selectedUser.value.id,
        page: userLogPage.value,
        size: userLogPageSize.value
      }
    })
    userLogs.value = r.data.items
    userLogTotal.value = r.data.total
  } catch (e) {
    ElMessage.error('获取用户日志失败')
  } finally {
    loadingUserLogs.value = false
  }
}

const getActionType = (action: string) => {
  if (action === 'PURCHASE') return 'success'
  if (action === 'BROWSE_PRODUCT') return 'info'
  return ''
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  fetchUsers()
  fetchLogs()
})
</script>

<style scoped>
.admin-users { padding: 20px; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
