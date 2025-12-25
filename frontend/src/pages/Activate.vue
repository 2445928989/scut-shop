<template>
  <div class="activate-container">
    <el-card class="activate-card">
      <div v-if="loading" class="loading-state">
        <p>正在验证激活码，请稍候...</p>
      </div>
      
      <el-result
        v-else
        :icon="success ? 'success' : 'error'"
        :title="success ? '激活成功' : '激活失败'"
        :sub-title="message"
      >
        <template #extra>
          <el-button v-if="success" type="primary" @click="router.push('/login')">
            立即登录
          </el-button>
          <el-button v-else type="primary" @click="router.push('/register')">
            返回注册
          </el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const success = ref(false)
const loading = ref(true)
const message = ref('')

onMounted(async () => {
  // 使用 route.query 获取参数，更符合 Vue Router 规范
  const token = route.query.token as string
  
  if (!token) {
    success.value = false
    message.value = '激活链接无效：缺少令牌'
    loading.value = false
    return
  }

  try {
    const r = await api.get('/api/auth/activate', { params: { token } })
    if (r.data && r.data.status === 'activated') {
      success.value = true
      message.value = '您的账号已成功激活，现在可以登录了。'
    } else {
      success.value = false
      message.value = '服务器返回了未知的响应状态'
    }
  } catch (e: any) {
    success.value = false
    // 优先显示后端返回的错误信息
    message.value = e?.response?.data?.error || e?.message || '激活过程中发生错误'
    console.error('Activation error:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.activate-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
  min-height: 50vh; /* 确保有足够高度显示内容 */
}
.activate-card {
  width: 100%;
  max-width: 500px;
  text-align: center;
}
.loading-state {
  padding: 40px;
  font-size: 18px;
  color: #909399;
}
.loading-state .el-icon {
  font-size: 40px;
  margin-bottom: 16px;
}
</style>
