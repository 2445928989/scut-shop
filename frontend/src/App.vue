<template>
  <el-container style="min-height:100vh">
    <el-header>
      <router-link to="/" class="header-brand">
        <h2>SCUT Shop</h2>
      </router-link>
      <div class="header-right">
        <router-link to="/">商品</router-link>
        <router-link to="/cart">购物车</router-link>
        <template v-if="!isAuth">
          <router-link to="/login">登录</router-link>
        </template>
        <template v-else>
          <router-link v-if="isAdmin" to="/admin/products">管理</router-link>
          <span class="username-pill">{{username}}</span>
          <el-button type="text" @click="logout">登出</el-button>
        </template>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
    <el-footer class="app-footer">
      <div class="footer-content">
        <p>网络工程 202330451741 王思哲</p>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const isAuth = computed(() => !!auth.accessToken)
const isAdmin = computed(() => auth.isAdmin)
const username = computed(() => auth.username)
if (auth.accessToken) { // fetch user info on app start when token present
  void auth.fetchMe()
}
function logout(){
  auth.logout()
}
</script>

<style>
/* Global styles are in global.css */
</style>