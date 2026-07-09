<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background:#1a3a2a">
      <div class="logo">🌲 林区防火系统</div>
      <el-menu
        router
        background-color="#1a3a2a"
        text-color="#bfd7c0"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>数据大屏
        </el-menu-item>
        <el-menu-item index="/forest">
          <el-icon><Location /></el-icon>林区管理
        </el-menu-item>
        <el-menu-item index="/detection/upload">
          <el-icon><Upload /></el-icon>AI识别
        </el-menu-item>
        <el-menu-item index="/detection/history">
          <el-icon><Document /></el-icon>识别记录
        </el-menu-item>
        <el-menu-item index="/workorder">
          <el-icon><List /></el-icon>工单管理
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Reading /></el-icon>防火知识
        </el-menu-item>
        <el-menu-item index="/notification">
          <el-icon><Bell /></el-icon>消息通知
          <el-badge :value="unreadCount" class="badge" />
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #eee">
        <span>欢迎，{{ user?.realName || '用户' }}</span>
        <el-button type="danger" text @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const user = ref(null)
const unreadCount = ref(0)

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) user.value = JSON.parse(userStr)
  fetchUnreadCount()
})

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/notification/unread-count')
    unreadCount.value = res.data.data || 0
  } catch (e) {}
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
  ElMessage.success('已退出')
}
</script>

<style scoped>
.logo {
  color: #fff;
  font-size: 18px;
  padding: 20px;
  text-align: center;
  font-weight: bold;
  border-bottom: 1px solid #2d5a3d;
}
.badge {
  margin-left: 10px;
}
</style>