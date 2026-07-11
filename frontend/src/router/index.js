import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Layout from '@/views/Layout.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: '/dashboard', component: () => import('@/views/dashboard/Index.vue') },
      { path: '/forest', component: () => import('@/views/forest/ForestArea.vue') },
      { path: '/detection/upload', component: () => import('@/views/detection/ImageUpload.vue') },
      { path: '/detection/history', component: () => import('@/views/detection/DetectionHistory.vue') },
      { path: '/workorder', component: () => import('@/views/workorder/WorkOrderList.vue') },
      { path: '/knowledge', component: () => import('@/views/knowledge/KnowledgeList.vue') },
      { path: '/notification', component: () => import('@/views/notification/NotificationList.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
