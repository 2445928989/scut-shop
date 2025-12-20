<template>
  <el-container style="min-height:100vh">
    <el-header>
      <el-row>
        <el-col :span="18">
          <router-link to="/" class="header-brand">
            <h2 style="color:var(--header-text);margin:0">SCUT Shop</h2>
          </router-link>
        </el-col>
        <el-col :span="6" class="header-right" style="text-align:right">
          <router-link to="/">商品</router-link>
          <span style="margin-left: 12px"></span>
          <router-link to="/cart">购物车</router-link>
          <span style="margin-left: 12px"></span>
          <template v-if="!isAuth">
            <router-link to="/login">登录</router-link>
          </template>
          <template v-else>
            <span class="username-pill">{{username}}</span>
            <el-button type="text" @click="logout" style="margin-left:12px">登出</el-button>
          </template>
        </el-col>
      </el-row>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const isAuth = computed(() => !!auth.accessToken)
const username = computed(() => auth.username)
function logout(){
  auth.logout()
}
</script>

<style>
.el-header{ /* replaced by global styles */ }
.el-header h2{ color:var(--header-text); margin:0; }
.header-right{ display:flex; gap:12px; align-items:center; justify-content:flex-end }
.username-pill{ display:inline-block }
</style>