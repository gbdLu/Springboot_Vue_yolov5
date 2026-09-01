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
          <el-menu-item index="/detection/realtime">
            <el-icon><VideoCamera /></el-icon>实时监控
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
          <el-menu-item index="/notification" class="notification-menu-item">
            <div class="notification-content">
              <el-icon><Bell /></el-icon>
              <span>消息通知</span>
              <span v-if="unreadCount > 0" class="notification-dot">{{ displayCount }}</span>
            </div>
          </el-menu-item>
          <el-sub-menu index="/system">
            <template #title>
              <el-icon><Setting /></el-icon>系统管理
            </template>
            <el-menu-item index="/system/users">
              <el-icon><User /></el-icon>用户管理
            </el-menu-item>
          </el-sub-menu>
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
          <el-menu-item index="/notification" class="notification-menu-item">
            <div class="notification-content">
              <el-icon><Bell /></el-icon>
              <span>消息通知</span>
              <span v-if="unreadCount > 0" class="notification-dot">{{ displayCount }}</span>
            </div>
          </el-menu-item>
        </template>

        <!-- GUARD: AI识别/工单/知识库 -->
        <template v-else>
          <el-menu-item index="/detection/upload">
            <el-icon><Upload /></el-icon>AI识别
          </el-menu-item>
          <el-menu-item index="/detection/realtime">
            <el-icon><VideoCamera /></el-icon>实时监控
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
          <el-menu-item index="/notification" class="notification-menu-item">
            <div class="notification-content">
              <el-icon><Bell /></el-icon>
              <span>消息通知</span>
              <span v-if="unreadCount > 0" class="notification-dot">{{ displayCount }}</span>
            </div>
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
            <span v-if="unreadCount > 0" class="unread-badge">{{ displayCount }}</span>
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
import { ref, computed, onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
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

// 计算显示的红点数字：0隐藏，1-99显示实际数字，>99显示99+
const displayCount = computed(() => {
  if (unreadCount.value <= 0) return 0
  if (unreadCount.value > 99) return '99+'
  return unreadCount.value
})

// 提供给子组件刷新未读数的方法
const refreshUnreadCount = async () => {
  await fetchUnreadCount()
}
provide('refreshUnreadCount', refreshUnreadCount)

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) user.value = JSON.parse(userStr)
  fetchUnreadCount()
})

// 监听路由变化，进入消息页面时刷新未读数
router.afterEach((to) => {
  if (to.path === '/notification' || to.path === '/') {
    fetchUnreadCount()
  }
})

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/notification/unread-count')
    const count = res.data.data || 0
    unreadCount.value = count
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
.notification-menu-item {
  padding: 0 20px !important;
}
.notification-content {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
}
.notification-content .el-icon {
  margin-right: 8px;
  font-size: 18px;
  flex-shrink: 0;
}
.notification-content > span:not(.notification-dot) {
  flex: 1;
}
.notification-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  border-radius: 10px;
  flex-shrink: 0;
  margin-left: 8px;
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
