<template>
  <div class="workorder-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ roleCode === 'GUARD' ? '📋 我的工单' : '📋 工单管理' }}</span>
          <div>
            <el-button v-if="roleCode !== 'GUARD'" @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>

      <!-- 状态筛选标签 -->
      <div class="status-tabs">
        <el-radio-group v-model="statusFilter" @change="fetchData">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button :label="1">待派单</el-radio-button>
          <el-radio-button :label="2">处置中</el-radio-button>
          <el-radio-button :label="3">待审核</el-radio-button>
          <el-radio-button :label="4">已办结</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="tableData" stripe v-loading="loading" style="margin-top:15px">
        <el-table-column prop="orderNo" label="工单编号" width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.orderType === 1 ? 'danger' : 'warning'">{{ row.orderType === 1 ? '一级紧急' : '二级普通' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forestAreaName" label="林区" />
        <el-table-column prop="hazardType" label="隐患类型" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.orderStatus)">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="roleCode !== 'GUARD'" prop="assignedToName" label="处置人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <!-- MANAGER: 指派（待派单状态） -->
            <el-button v-if="roleCode === 'MANAGER' && row.orderStatus === 1" size="small" type="primary" @click="showAssign(row)">指派</el-button>
            <!-- GUARD: 填写反馈（处置中状态，且是自己的工单） -->
            <el-button v-if="roleCode === 'GUARD' && row.orderStatus === 2" size="small" type="warning" @click="showFeedback(row)">填写反馈</el-button>
            <!-- MANAGER: 审核（待审核状态） -->
            <el-button v-if="roleCode === 'MANAGER' && row.orderStatus === 3" size="small" type="success" @click="showReview(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" style="margin-top:15px;justify-content:flex-end" @current-change="fetchData"
      />
    </el-card>

    <!-- 指派对话框（MANAGER） -->
    <el-dialog v-model="assignVisible" title="指派工单" width="400px">
      <el-form label-width="80px">
        <el-form-item label="指派给">
          <el-select v-model="assignForm.assignedTo" placeholder="选择处置人">
            <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 填写反馈对话框（GUARD） -->
    <el-dialog v-model="feedbackVisible" title="填写反馈" width="600px">
      <el-form label-width="100px">
        <el-form-item label="处置描述" required>
          <el-input v-model="feedbackForm.disposalDesc" type="textarea" :rows="4" placeholder="请描述处置情况" />
        </el-form-item>
        <el-form-item label="整改复查照片">
          <el-upload
            action="/api/upload/image"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemoveImage"
            :file-list="feedbackForm.images"
            list-type="picture-card"
            :limit="5"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div style="color:#999;font-size:12px">最多上传5张照片</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFeedback" :loading="feedbackLoading">提交反馈</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框（MANAGER） -->
    <el-dialog v-model="reviewVisible" title="审核工单" width="500px">
      <el-form label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.reviewResult">
            <el-radio :value="1">合格（办结）</el-radio>
            <el-radio :value="2">不合格（退回）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReview">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="林区">{{ detail.forestAreaName }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detail.orderType === 1 ? 'danger' : 'warning'">{{ detail.orderType === 1 ? '一级紧急' : '二级普通' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.orderStatus)">{{ statusText(detail.orderStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="隐患类型">{{ detail.hazardType }}</el-descriptions-item>
        <el-descriptions-item label="指派人">{{ detail.assignedByName }}</el-descriptions-item>
        <el-descriptions-item label="处置人">{{ detail.assignedToName }}</el-descriptions-item>
        <el-descriptions-item label="处置时间">{{ detail.disposalAt }}</el-descriptions-item>
        <el-descriptions-item label="隐患描述" :span="2">{{ detail.hazardDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.detectionImageResult" label="识别结果图" :span="2">
          <img :src="detail.detectionImageResult" style="max-width:100%;max-height:300px;border-radius:4px;cursor:pointer"
            @click="previewImage(detail.detectionImageResult)"
            @error="(e) => e.target.src = detail.detectionImageOriginal" />
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.disposalDesc" label="处置描述" :span="2">{{ detail.disposalDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.disposalImages" label="处置照片" :span="2">
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <img v-for="(img, i) in parseImages(detail.disposalImages)" :key="i" :src="img"
              style="width:100px;height:100px;object-fit:cover;border-radius:4px;cursor:pointer"
              @click="previewImage(img)" />
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.reviewComment" label="审核意见" :span="2">{{ detail.reviewComment }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="imagePreviewVisible" width="80%" :show-close="true">
      <img :src="previewImageUrl" style="width:100%" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getWorkOrderList, getMyOrders, getWorkOrderDetail, assignWorkOrder, disposeWorkOrder, reviewWorkOrder, exportWorkOrders } from '@/api/workOrder'
import { getGuards } from '@/api/auth'
import request from '@/utils/request'

const roleCode = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.roleCode || 'GUARD'
})

const token = localStorage.getItem('token')
const uploadHeaders = computed(() => ({
  Authorization: token ? `Bearer ${token}` : ''
}))

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

const assignVisible = ref(false)
const feedbackVisible = ref(false)
const reviewVisible = ref(false)
const detailVisible = ref(false)
const imagePreviewVisible = ref(false)
const previewImageUrl = ref('')
const detail = ref({})
const users = ref([])
const feedbackLoading = ref(false)

const assignForm = ref({ orderId: null, assignedTo: null })
const feedbackForm = ref({ orderId: null, disposalDesc: '', images: [] })
const reviewForm = ref({ orderId: null, reviewResult: 1, reviewComment: '' })

const statusText = (s) => ['', '待派单', '处置中', '待审核', '已办结'][s] || '未知'
const statusType = (s) => ['', 'info', 'warning', 'success', ''][s] || 'info'

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderStatus: statusFilter.value || undefined
    }
    let res
    if (roleCode.value === 'GUARD') {
      res = await getMyOrders(params)
    } else {
      res = await getWorkOrderList(params)
    }
    if (res.data.code === 200) {
      tableData.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row) => {
  const res = await getWorkOrderDetail(row.id)
  if (res.data.code === 200) {
    detail.value = res.data.data
    detailVisible.value = true
  } else {
    ElMessage.error(res.data.message)
  }
}

const parseImages = (imagesStr) => {
  if (!imagesStr) return []
  return imagesStr.split(',').filter(Boolean)
}

const previewImage = (url) => {
  previewImageUrl.value = url
  imagePreviewVisible.value = true
}

// 指派工单
const showAssign = async (row) => {
  assignForm.value = { orderId: row.id, assignedTo: null }
  const res = await getGuards()
  if (res.data.code === 200) users.value = res.data.data
  assignVisible.value = true
}

const handleAssign = async () => {
  if (!assignForm.value.assignedTo) {
    ElMessage.warning('请选择处置人')
    return
  }
  const res = await assignWorkOrder(assignForm.value)
  if (res.data.code === 200) {
    ElMessage.success('指派成功')
    assignVisible.value = false
    fetchData()
  } else {
    ElMessage.error(res.data.message)
  }
}

// 填写反馈
const showFeedback = (row) => {
  feedbackForm.value = {
    orderId: row.id,
    disposalDesc: '',
    images: []
  }
  feedbackVisible.value = true
}

const handleUploadSuccess = (response, file) => {
  if (response.code === 200) {
    feedbackForm.value.images.push({ url: response.data, name: file.name })
  } else {
    ElMessage.error('上传失败')
  }
}

const handleRemoveImage = (file) => {
  feedbackForm.value.images = feedbackForm.value.images.filter(img => img.url !== file.url)
}

const handleFeedback = async () => {
  if (!feedbackForm.value.disposalDesc.trim()) {
    ElMessage.warning('请填写处置描述')
    return
  }
  feedbackLoading.value = true
  try {
    const imageUrls = feedbackForm.value.images.map(img => img.url).join(',')
    const res = await disposeWorkOrder({
      orderId: feedbackForm.value.orderId,
      disposalDesc: feedbackForm.value.disposalDesc,
      disposalImages: imageUrls
    })
    if (res.data.code === 200) {
      ElMessage.success('反馈提交成功，等待审核')
      feedbackVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.data.message)
    }
  } finally {
    feedbackLoading.value = false
  }
}

// 审核工单
const showReview = (row) => {
  reviewForm.value = { orderId: row.id, reviewResult: 1, reviewComment: '' }
  reviewVisible.value = true
}

const handleReview = async () => {
  const res = await reviewWorkOrder(reviewForm.value)
  if (res.data.code === 200) {
    ElMessage.success('审核完成')
    reviewVisible.value = false
    fetchData()
  } else {
    ElMessage.error(res.data.message)
  }
}

// 导出
const handleExport = async () => {
  const res = await exportWorkOrders()
  if (res.data.code === 200) {
    ElMessage.success('导出成功，共' + res.data.data.length + '条')
  } else {
    ElMessage.error(res.data.message)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.workorder-container {
  padding: 20px;
}
.status-tabs {
  margin-bottom: 15px;
}
</style>
