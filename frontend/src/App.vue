<template>
  <div class="app-container">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <el-icon size="32" color="#409eff"><Monitor /></el-icon>
          <span>设备管理系统</span>
        </div>
        <el-menu :default-active="activeMenu" class="menu" router>
          <el-menu-item index="/">
            <el-icon><PieChart /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon>
            <span>设备列表</span>
          </el-menu-item>
          <el-menu-item index="/grouped">
            <el-icon><Grid /></el-icon>
            <span>楼层分组</span>
          </el-menu-item>
          <el-menu-item index="/floor">
            <el-icon><OfficeBuilding /></el-icon>
            <span>楼层管理</span>
          </el-menu-item>
          <el-menu-item index="/room">
            <el-icon><HomeFilled /></el-icon>
            <span>接待室管理</span>
          </el-menu-item>
          <el-menu-item index="/transfer">
            <el-icon><RefreshLeft /></el-icon>
            <span>流转记录</span>
          </el-menu-item>
          <el-menu-item index="/spec-template">
            <el-icon><Tickets /></el-icon>
            <span>规格模板</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <el-button @click="toggleSidebar" class="toggle-btn">
            <el-icon><Menu /></el-icon>
          </el-button>
          <span class="title">{{ pageTitle }}</span>
          <div class="header-right">
            <span>{{ currentUser }}</span>
          </div>
        </el-header>
        <el-main class="main">
          <router-view @update-title="updateTitle" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Monitor, PieChart, Grid, OfficeBuilding, HomeFilled, RefreshLeft, Menu, Tickets } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeMenu = ref('/')
const pageTitle = ref('首页')
const currentUser = ref('管理员')
const sidebarCollapsed = ref(false)

const titleMap = {
  '/': '设备管理系统',
  '/device': '设备列表',
  '/device/add': '添加设备',
  '/device/edit': '编辑设备',
  '/device/detail': '设备详情',
  '/floor': '楼层管理',
  '/room': '接待室管理',
  '/transfer': '流转记录',
  '/spec-template': '设备规格模板管理',
  '/grouped': '楼层分组视图'
}

const updateTitle = (title) => {
  pageTitle.value = title
}

onMounted(() => {
  activeMenu.value = route.path
  pageTitle.value = titleMap[route.path] || '设备管理系统'
})

onUnmounted(() => {})

router.beforeEach((to, from, next) => {
  activeMenu.value = to.path.split('/edit')[0].split('/detail')[0]
  pageTitle.value = titleMap[to.path] || titleMap[to.path.split('/edit')[0]] || titleMap[to.path.split('/detail')[0]] || '设备管理系统'
  next()
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  transition: width 0.3s;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  gap: 10px;
}

.menu {
  border-right: none;
  margin-top: 20px;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8);
  height: 48px;
  line-height: 48px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
}

.menu :deep(.el-menu-item.is-active) {
  background: #409eff;
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.toggle-btn {
  border: none;
  background: none;
  margin-right: 16px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main {
  background: #f5f5f5;
  padding: 20px;
}
</style>
