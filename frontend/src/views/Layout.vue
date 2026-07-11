<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background:#1a3a2a">
      <div class="logo">🌲 林区防火系统</div>
      <el-menu
        router
        background-color="#1a3a2a"
        text-color="#bfd7c0"
        active-text-color="#fff"
        :default-active="$route.path"
      >
        <!-- ADMIN: 全部菜单 -->
        <template v-if="roleCode === 'ADMIN'">
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
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="badge" />
          </el-menu-item>
        </template>

        <!-- MANAGER: 林区管理/工单/统计/知识库 -->
        <template v-else-if="roleCode === 'MANAGER'">
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>数据大屏
          </el-menu-item>
          <el-menu-item index="/forest">
            <el-icon><Location /></el-icon>林区管理
          </el-menu-item>
          <el-menu-item index="/workorder">
            <el-icon><List /></el-icon>工单管理
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Reading /></el-icon>防火知识
          </el-menu-item>
          <el-menu-item index="/notification">
            <el-icon><Bell /></el-icon>消息通知
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="badge" />
          </el-menu-item>
        </template>

        <!-- GUARD: AI识别/工单/知识库 -->
        <template v-else>
          <el-menu-item index="/detection/upload">
            <el-icon><Upload /></el-icon>AI识别
          </el-menu-item>
          <el-menu-item index="/detection/history">
            <el-icon><Document /></el-icon>识别记录
          </el-menu-item>
          <el-menu-item index="/workorder">
            <el-icon><List /></el-icon>我的工单
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Reading /></el-icon>防火知识
          </el-menu-item>
          <el-menu-item index="/notification">
            <el-icon><Bell /></el-icon>消息通知
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="badge" />
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #eee">
        <div>
          <span>欢迎，{{ user?.realName || '用户' }}</span>
          <el-tag size="small" style="margin-left:10px" :type="roleTagType">{{ roleLabel }}</el-tag>
        </div>
        <div>
          <el-button text @click="router.push('/notification')">
            <el-icon><Bell /></el-icon>
            <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
          </el-button>
          <el-button type="danger" text @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const user = ref(null)
const unreadCount = ref(0)

const roleCode = computed(() => user.value?.roleCode || 'GUARD')
const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', MANAGER: '林区管理员', GUARD: '巡护员' }
  return map[roleCode.value] || '未知'
})
const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', MANAGER: 'warning', GUARD: 'success' }
  return map[roleCode.value] || 'info'
})

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
.unread-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  margin-left: 4px;
}
</style>
