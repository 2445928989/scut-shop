<template>
  <el-card class="login-card">
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSubmit">登录</el-button>
        <el-button type="text" @click="toRegister">没有账号，去注册</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ username: '', password: '' })

async function onSubmit(){
  try{
    await auth.login(form.username, form.password)
    router.push('/')
  }catch(e){
    console.error(e)
    alert('登录失败')
  }
}

function toRegister(){
  router.push('/register')
}
</script>

<style scoped>
.login-card{ max-width:400px; margin:40px auto }
</style>