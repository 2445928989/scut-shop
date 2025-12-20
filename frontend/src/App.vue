<template>
  <el-container style="min-height:100vh">
    <el-header>
      <el-row>
        <el-col :span="18"><h2 style="color:white">SCUT Shop</h2></el-col>
        <el-col :span="6" style="text-align:right">
          <router-link to="/">商品</router-link>
          <span style="margin-left: 12px"></span>
          <router-link to="/cart">购物车</router-link>
          <span style="margin-left: 12px"></span>
          <template v-if="!isAuth">
            <router-link to="/login">登录</router-link>
          </template>
          <template v-else>
            <span>{{username}}</span>
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
import { computed } from 'vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const isAuth = computed(() => !!auth.accessToken)
const username = computed(() => auth.username)
function logout(){
  auth.logout()
}
</script>

<style>
.el-header{ background:#409EFF; padding:10px 20px }
</style>