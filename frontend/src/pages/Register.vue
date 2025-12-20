<template>
  <el-card class="register-card">
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="电子邮件">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSubmit">注册并登录</el-button>
        <el-button type="text" @click="toLogin">已有账号，去登录</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ username: '', email: '', password: '' })

async function onSubmit(){
  try{
    const r = await auth.register(form.username, form.email, form.password)
    if (r && r.activation === 'sent') {
      ElMessage.success('已发送激活邮件，请检查邮箱并完成激活后登录')
      router.push('/')
      return
    }
    if (r && r.error) {
      // backend error codes -> user-friendly messages
      if (r.error === 'email_exists') {
        ElMessage.error('该邮箱已被注册，请尝试登录或使用其他邮箱')
      } else if (r.error === 'username_exists') {
        ElMessage.error('该用户名已被占用，请选择其他用户名')
      } else {
        ElMessage.error('注册失败：' + r.error)
      }
      return
    }

    ElMessage.success('注册并已登录')
    router.push('/')
  }catch(e){
    console.error(e)
    ElMessage.error('注册或登录过程中发生错误，请稍后再试')
  }
}

function toLogin(){
  router.push('/login')
}
</script>

<style scoped>
.register-card{ max-width:500px; margin:40px auto }
</style>
