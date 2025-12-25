<template>
  <div>
    <!-- 权限检查：如果没有管理员权限，显示登录提示 -->
    <el-empty v-if="!isAdmin" description="请登录后访问">
      <template #default>
        <p style="margin-bottom:12px">您需要登录才能访问此页面</p>
        <router-link to="/login">
          <el-button type="primary">前往登录</el-button>
        </router-link>
      </template>
    </el-empty>

    <!-- 如果有权限，显示商品管理界面 -->
    <div v-else>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h3 style="margin: 0;">商品管理</h3>
        <el-button type="primary" @click="openCreate">新建商品</el-button>
      </div>

      <!-- 搜索和筛选区域 -->
      <div style="margin-bottom: 20px; padding: 20px; background: #f8f9fa; border-radius: 8px;">
        <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索商品名称或SKU" 
            style="width: 300px;"
            clearable
            @keyup.enter="handleSearch"
          />
          
          <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 120px;" clearable>
            <el-option label="全部" :value="-1" />
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
          
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="info" @click="refreshData" :loading="loading">
            刷新
          </el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 20px;">
        <div style="padding: 20px; background: white; border: 1px solid #ebeef5; border-radius: 8px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; color: #666; margin-bottom: 4px;">商品总数</div>
              <div style="font-size: 24px; font-weight: bold; color: #409eff">{{ products.length }}</div>
            </div>
            <span style="font-size: 14px; color: #409eff;">总数</span>
          </div>
        </div>
        
        <div style="padding: 20px; background: white; border: 1px solid #ebeef5; border-radius: 8px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; color: #666; margin-bottom: 4px;">上架商品</div>
              <div style="font-size: 24px; font-weight: bold; color: #67c23a">{{ onSaleCount }}</div>
            </div>
            <span style="font-size: 14px; color: #67c23a;">上架</span>
          </div>
        </div>
        
        <div style="padding: 20px; background: white; border: 1px solid #ebeef5; border-radius: 8px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; color: #666; margin-bottom: 4px;">下架商品</div>
              <div style="font-size: 24px; font-weight: bold; color: #909399">{{ offSaleCount }}</div>
            </div>
            <span style="font-size: 14px; color: #909399;">下架</span>
          </div>
        </div>
        
        <div style="padding: 20px; background: white; border: 1px solid #ebeef5; border-radius: 8px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <div style="font-size: 14px; color: #666; margin-bottom: 4px;">总库存</div>
              <div style="font-size: 24px; font-weight: bold; color: #e6a23c">{{ totalStock }}</div>
            </div>
            <span style="font-size: 14px; color: #e6a23c;">库存</span>
          </div>
        </div>
      </div>

      <!-- 批量操作 -->
      <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px;">
        <el-button 
          type="danger" 
          :disabled="selectedProducts.length === 0"
          @click="batchDelete"
        >
          批量下架
        </el-button>
        <el-button 
          type="success" 
          :disabled="selectedProducts.length === 0"
          @click="batchRestore"
        >
          批量恢复
        </el-button>
        <span v-if="selectedProducts.length > 0" style="color: #666; font-size: 14px;">
          已选择 {{ selectedProducts.length }} 个商品
        </span>
      </div>

      <el-table 
        :data="products" 
        style="width:100%" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column prop="name" label="名称" sortable />
        <el-table-column prop="sku" label="SKU" width="120" sortable />
        <el-table-column prop="price" label="价格" width="120" sortable>
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" sortable />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">上架</el-tag>
            <el-tag v-else type="info">已下架</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" @click="openQuickEdit(row)">快速编辑</el-button>
              <el-button size="small" v-if="row.status === 1" type="danger" @click="del(row.id)">下架</el-button>
              <el-button size="small" v-else type="primary" @click="restore(row.id)">恢复</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新建/编辑对话框 -->
      <el-dialog :title="editId ? '编辑商品' : '新建商品'" v-model="showDialog" width="800px">
        <el-form :model="form" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="商品名称" required>
                <el-input v-model="form.name" placeholder="请输入商品名称" />
              </el-form-item>
              <el-form-item label="SKU编码">
                <el-input v-model="form.sku" placeholder="请输入SKU编码" />
              </el-form-item>
              <el-form-item label="价格" required>
                <el-input v-model.number="form.price" type="number" placeholder="请输入价格">
                  <template #prefix>¥</template>
                </el-input>
              </el-form-item>
              <el-form-item label="库存" required>
                <el-input v-model.number="form.stock" type="number" placeholder="请输入库存数量" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="form.status" placeholder="请选择状态">
                  <el-option label="上架" :value="1" />
                  <el-option label="下架" :value="0" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商品图片">
                <div style="margin-bottom: 8px;">
                  <el-input v-model="form.imageUrl" placeholder="请输入图片URL" />
                </div>
                <div v-if="form.imageUrl" style="width: 100%; height: 200px; border: 1px solid #dcdfe6; border-radius: 4px; overflow: hidden;">
                  <img :src="form.imageUrl" alt="商品图片预览" style="width: 100%; height: 100%; object-fit: contain;" />
                </div>
                <div v-else style="width: 100%; height: 200px; border: 1px dashed #dcdfe6; border-radius: 4px; display: flex; align-items: center; justify-content: center; color: #999;">
                  暂无图片
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="商品描述">
            <el-input type="textarea" v-model="form.description" :rows="4" placeholder="请输入商品详细描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showDialog=false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>

      <!-- 快速编辑对话框 -->
      <el-dialog title="快速编辑" v-model="showQuickEditDialog" width="500px">
        <el-form :model="quickEditForm" label-width="80px">
          <el-form-item label="商品名称">
            <el-input v-model="quickEditForm.name" />
          </el-form-item>
          <el-form-item label="价格">
            <el-input v-model.number="quickEditForm.price" type="number">
              <template #prefix>¥</template>
            </el-input>
          </el-form-item>
          <el-form-item label="库存">
            <el-input v-model.number="quickEditForm.stock" type="number" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="quickEditForm.status">
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="0" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showQuickEditDialog=false">取消</el-button>
          <el-button type="primary" @click="saveQuickEdit">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '../api'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)

const products = ref<any[]>([])
const loading = ref(false)
const showDialog = ref(false)
const showQuickEditDialog = ref(false)
const editId = ref<number | null>(null)
const quickEditId = ref<number | null>(null)
const form = ref<any>({ name: '', sku: '', price: 0, stock: 0, imageUrl: '', description: '', status: 1 })
const quickEditForm = ref<any>({ name: '', price: 0, stock: 0, status: 1 })

// 搜索相关
const searchKeyword = ref('')
const filterStatus = ref(-1)

// 批量操作
const selectedProducts = ref<any[]>([])

// 计算属性
const onSaleCount = computed(() => {
  return products.value.filter(p => p.status === 1).length
})

const offSaleCount = computed(() => {
  return products.value.filter(p => p.status === 0).length
})

const totalStock = computed(() => {
  return products.value.reduce((sum, p) => sum + (p.stock || 0), 0)
})

async function load() {
  loading.value = true
  try {
    const params: any = {}
    
    // 管理界面：显式指定status参数
    // filterStatus=1: 只显示上架商品
    // filterStatus=0: 只显示下架商品
    // filterStatus=-1: 显示所有商品
    params.status = filterStatus.value
    
    if (searchKeyword.value.trim()) {
      params.q = searchKeyword.value.trim()
    }
    
    params.page = 1
    params.size = 100
    
    // 公开端点，用于查询商品
    const r = await api.get('/api/products', { params })
    products.value = r.data && r.data.items ? r.data.items : r.data
  } catch (error: any) {
    ElMessage.error('加载商品列表失败')
    console.error('加载商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 如果有token但还没加载authorities，先加载
  if (auth.accessToken && auth.authorities.length === 0) {
    await auth.fetchMe()
  }
  
  // 加载权限后再检查和加载数据
  if (isAdmin.value) {
    await load()
  }
})

function openCreate() {
  editId.value = null
  form.value = { name: '', sku: '', price: 0, stock: 0, imageUrl: '', description: '', status: 1 }
  showDialog.value = true
}

function openEdit(row: any) {
  editId.value = row.id
  form.value = { ...row }
  showDialog.value = true
}

function openQuickEdit(row: any) {
  quickEditId.value = row.id
  quickEditForm.value = { 
    name: row.name, 
    price: row.price, 
    stock: row.stock, 
    status: row.status 
  }
  showQuickEditDialog.value = true
}

function validateForm() {
  if (!form.value.name || form.value.name.trim() === '') {
    ElMessage.error('请输入商品名称')
    return false
  }
  
  if (!form.value.price || form.value.price <= 0) {
    ElMessage.error('请输入有效的价格')
    return false
  }
  
  if (form.value.stock === undefined || form.value.stock < 0) {
    ElMessage.error('请输入有效的库存数量')
    return false
  }
  
  return true
}

async function save() {
  if (!validateForm()) {
    return
  }
  
  try {
    if (editId.value) {
      await api.put(`/api/admin/products/${editId.value}`, form.value)
      ElMessage.success('商品更新成功')
      showDialog.value = false
      await load()
    } else {
      const r = await api.post('/api/admin/products', form.value)
      if (r.data && r.data.id) {
        ElMessage.success('商品创建成功')
        showDialog.value = false
        await load()
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.error || '保存失败')
  }
}

async function saveQuickEdit() {
  try {
    if (quickEditId.value) {
      await api.put(`/api/admin/products/${quickEditId.value}`, quickEditForm.value)
      ElMessage.success('快速编辑成功')
      showQuickEditDialog.value = false
      await load()
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.error || '保存失败')
  }
}

async function del(id: number) {
  try {
    await ElMessageBox.confirm('确认下架该商品？下架后用户将无法看到此商品。', '确认下架', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    const r = await api.delete(`/api/admin/products/${id}`)
    ElMessage.success(r.data && r.data.status === 'deleted' ? '下架成功' : '下架成功')
    await load()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = error && error.response && error.response.data && error.response.data.error ? error.response.data.error : '下架失败'
      ElMessage.error(msg)
    }
  }
}

async function restore(id: number) {
  try {
    await ElMessageBox.confirm('确认恢复该商品？恢复后用户将可以正常看到此商品。', '确认恢复', {
      confirmButtonText: '确认恢复',
      cancelButtonText: '取消',
      type: 'info',
    })
    
    await api.put(`/api/admin/products/${id}`, { status: 1 })
    ElMessage.success('恢复成功')
    await load()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = error && error.response && error.response.data && error.response.data.error ? error.response.data.error : '恢复失败'
      ElMessage.error(msg)
    }
  }
}

function handleSearch() {
  load()
}

function resetSearch() {
  searchKeyword.value = ''
  filterStatus.value = -1
  load()
}

async function refreshData() {
  await load()
  ElMessage.success('数据已刷新')
}

function handleSelectionChange(selection: any[]) {
  selectedProducts.value = selection
}

async function batchDelete() {
  if (selectedProducts.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确认下架选中的 ${selectedProducts.value.length} 个商品？`, '批量下架', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    const promises = selectedProducts.value.map(product => 
      api.delete(`/api/admin/products/${product.id}`)
    )
    
    await Promise.all(promises)
    ElMessage.success(`成功下架 ${selectedProducts.value.length} 个商品`)
    selectedProducts.value = []
    await load()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量下架失败')
    }
  }
}

async function batchRestore() {
  if (selectedProducts.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(`确认恢复选中的 ${selectedProducts.value.length} 个商品？`, '批量恢复', {
      confirmButtonText: '确认恢复',
      cancelButtonText: '取消',
      type: 'info',
    })
    
    const promises = selectedProducts.value.map(product => 
      api.put(`/api/admin/products/${product.id}`, { status: 1 })
    )
    
    await Promise.all(promises)
    ElMessage.success(`成功恢复 ${selectedProducts.value.length} 个商品`)
    selectedProducts.value = []
    await load()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('批量恢复失败')
    }
  }
}
</script>

<style scoped>
.el-table {
  margin-top: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.action-buttons :deep(.el-button) {
  flex: 1;
  min-width: 80px;
  margin: 0;
  transition: all 0.3s ease;
}

.action-buttons :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}
</style>
