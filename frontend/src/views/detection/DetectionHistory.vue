<template>
  <div class="history-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>📋 识别记录</span>
          <el-select v-model="forestAreaId" placeholder="筛选林区" clearable style="width:200px" @change="fetchData">
            <el-option v-for="a in areas" :key="a.id" :label="a.areaName" :value="a.id" />
          </el-select>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="forestAreaName" label="林区" />
        <el-table-column label="识别结果" width="200">
          <template #default="{ row }">
            <el-tag type="danger" size="small" style="margin-right:5px">🔥 {{ row.fireCount }}</el-tag>
            <el-tag type="success" size="small" style="margin-right:5px">👤 {{ row.humanCount }}</el-tag>
            <el-tag type="warning" size="small">💨 {{ row.smokeCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="uploadUserName" label="上传者" width="100" />
        <el-table-column prop="detectionTime" label="识别时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top:15px;justify-content:flex-end"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="识别详情" width="800px">
      <div class="detail-stats">
        <el-statistic title="🔥 明火" :value="detail.fireCount || 0" />
        <el-statistic title="👤 人员" :value="detail.humanCount || 0" />
        <el-statistic title="💨 烟雾" :value="detail.smokeCount || 0" />
      </div>
      <div v-if="detail.id" style="margin-top:15px">
        <h4>识别结果图：</h4>
        <img :src="`/api/detection/result-image/${detail.id}`" style="max-width:100%;border-radius:8px"
             @error="(e) => e.target.src = `/api/detection/image/${detail.id}`" />
      </div>
      <div v-if="detail.resultJson" style="margin-top:15px">
        <h4>检测目标详情：</h4>
        <el-tag v-for="(det, i) in parseDetections(detail.resultJson)" :key="i"
          :type="det.class_name === 'fire' ? 'danger' : det.class_name === 'smoke' ? 'warning' : 'success'"
          style="margin:3px">
          {{ det.class_name }} {{ (det.confidence * 100).toFixed(1) }}%
        </el-tag>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDetectionHistory, getDetectionDetail, deleteDetection } from '@/api/detection'
import { getForestList } from '@/api/forest'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const forestAreaId = ref(null)
const areas = ref([])
const detailVisible = ref(false)
const detail = ref({})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDetectionHistory({ pageNum: pageNum.value, pageSize: pageSize.value, forestAreaId: forestAreaId.value })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally { loading.value = false }
}

const viewDetail = async (row) => {
  const res = await getDetectionDetail(row.id)
  if (res.data.code === 200) {
    detail.value = res.data.data
    detailVisible.value = true
  }
}

const parseDetections = (json) => {
  try {
    const data = JSON.parse(json)
    return data.detections || []
  } catch { return [] }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    const res = await deleteDetection(row.id)
    if (res.data.code === 200) { ElMessage.success('删除成功'); fetchData() }
  })
}

onMounted(async () => {
  fetchData()
  const res = await getForestList({ pageNum: 1, pageSize: 100 })
  if (res.data.code === 200) areas.value = res.data.data.records
})
</script>

<style scoped>
.history-container { padding: 20px; }
.detail-stats { display: flex; gap: 30px; }
</style>
