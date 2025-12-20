<template>
  <el-card class="activate-card">
    <div v-if="loading">正在验证激活码…</div>
    <div v-else>
      <div v-if="success">
        <h3>激活成功 ✅</h3>
        <el-button type="primary" @click="toLogin">去登录</el-button>
      </div>
      <div v-else>
        <h3>激活失败 ⚠️</h3>
        <p>{{ message }}</p>
        <el-button type="primary" @click="toRegister">返回注册</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const success = ref(false)
const loading = ref(true)
const message = ref('')

function toLogin(){ router.push('/login') }
function toRegister(){ router.push('/register') }

onMounted(async () => {
  const url = new URL(window.location.href)
  const token = url.searchParams.get('token')
  if (!token) {
    success.value = false
    message.value = '缺少激活码'
    loading.value = false
    return
  }
  try {
    const r = await api.get('/api/auth/activate', { params: { token } })
    if (r.data && r.data.status === 'activated') {
      success.value = true
    } else {
      success.value = false
      message.value = '未知响应'
    }
  } catch (e: any) {
    success.value = false
    message.value = e?.response?.data?.error || e?.message || '激活失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.activate-card{ max-width:640px; margin:40px auto; padding:20px }
</style>
