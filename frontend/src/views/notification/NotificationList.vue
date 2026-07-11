<template>
  <div class="notification-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>🔔 消息通知</span>
          <el-button @click="handleMarkAllRead">全部已读</el-button>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column label="" width="50">
          <template #default="{ row }">
            <el-badge :is-dot="row.isRead === 0" type="danger" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'work_order' ? 'warning' : 'info'">{{ row.type === 'work_order' ? '工单' : '系统' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.isRead === 0" size="small" @click="handleRead(row)">标为已读</el-button>
            <el-button v-if="row.relatedId && row.type === 'work_order'" size="small" type="primary" @click="goToOrder(row)">查看工单</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" style="margin-top:15px;justify-content:flex-end" @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getNotificationList, markAsRead, markAllRead, deleteNotification } from '@/api/notification'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getNotificationList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.data.code === 200) { tableData.value = res.data.data.records; total.value = res.data.data.total }
  } finally { loading.value = false }
}

const handleRead = async (row) => {
  await markAsRead(row.id)
  row.isRead = 1
  ElMessage.success('已标为已读')
}

const handleMarkAllRead = async () => {
  await markAllRead()
  ElMessage.success('全部已读')
  fetchData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteNotification(row.id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

const goToOrder = (row) => {
  router.push('/workorder')
}

onMounted(fetchData)
</script>

<style scoped>
.notification-container { padding: 20px; }
</style>
