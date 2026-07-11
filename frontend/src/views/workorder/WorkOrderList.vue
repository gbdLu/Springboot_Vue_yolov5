<template>
  <div class="workorder-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>📋 工单管理</span>
          <div>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:120px;margin-right:10px" @change="fetchData">
              <el-option label="待指派" :value="1" /><el-option label="已指派" :value="2" />
              <el-option label="已处置" :value="3" /><el-option label="已归档" :value="4" />
            </el-select>
            <el-select v-model="typeFilter" placeholder="类型筛选" clearable style="width:120px;margin-right:10px" @change="fetchData">
              <el-option label="一级紧急" :value="1" /><el-option label="二级普通" :value="2" />
            </el-select>
            <el-button v-if="roleCode !== 'GUARD'" @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
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
        <el-table-column prop="assignedToName" label="处置人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <!-- MANAGER: 指派（待指派状态） -->
            <el-button v-if="roleCode === 'MANAGER' && row.orderStatus === 1" size="small" type="primary" @click="showAssign(row)">指派</el-button>
            <!-- GUARD: 处置（已指派状态，且是自己的工单） -->
            <el-button v-if="roleCode === 'GUARD' && row.orderStatus === 2" size="small" type="warning" @click="showDispose(row)">处置</el-button>
            <!-- MANAGER: 审核（已处置状态） -->
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

    <!-- 处置对话框（GUARD） -->
    <el-dialog v-model="disposeVisible" title="处置工单" width="500px">
      <el-form label-width="80px">
        <el-form-item label="处置描述"><el-input v-model="disposeForm.disposalDesc" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="处置图片"><el-input v-model="disposeForm.disposalImages" placeholder="图片URL，多个用逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disposeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDispose">提交处置</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框（MANAGER） -->
    <el-dialog v-model="reviewVisible" title="审核工单" width="400px">
      <el-form label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.reviewResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">退回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见"><el-input v-model="reviewForm.reviewComment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReview">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="林区">{{ detail.forestAreaName }}</el-descriptions-item>
        <el-descriptions-item label="类型"><el-tag :type="detail.orderType === 1 ? 'danger' : 'warning'">{{ detail.orderType === 1 ? '一级紧急' : '二级普通' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.orderStatus)">{{ statusText(detail.orderStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="隐患类型">{{ detail.hazardType }}</el-descriptions-item>
        <el-descriptions-item label="指派人">{{ detail.assignedByName }}</el-descriptions-item>
        <el-descriptions-item label="处置人">{{ detail.assignedToName }}</el-descriptions-item>
        <el-descriptions-item label="处置时间">{{ detail.disposalAt }}</el-descriptions-item>
        <el-descriptions-item label="隐患描述" :span="2">{{ detail.hazardDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.disposalDesc" label="处置描述" :span="2">{{ detail.disposalDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.reviewComment" label="审核意见" :span="2">{{ detail.reviewComment }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkOrderList, getWorkOrderDetail, assignWorkOrder, disposeWorkOrder, reviewWorkOrder, exportWorkOrders } from '@/api/workOrder'
import { getGuards } from '@/api/auth'

const roleCode = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.roleCode || 'GUARD'
})

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)
const typeFilter = ref(null)

const assignVisible = ref(false)
const disposeVisible = ref(false)
const reviewVisible = ref(false)
const detailVisible = ref(false)
const detail = ref({})
const users = ref([])

const assignForm = ref({ orderId: null, assignedTo: null })
const disposeForm = ref({ orderId: null, disposalDesc: '', disposalImages: '' })
const reviewForm = ref({ orderId: null, reviewResult: 1, reviewComment: '' })

const statusText = (s) => ['', '待指派', '已指派', '已处置', '已归档'][s] || '未知'
const statusType = (s) => ['', 'info', 'warning', 'success', ''][s] || 'info'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getWorkOrderList({ pageNum: pageNum.value, pageSize: pageSize.value, orderStatus: statusFilter.value, orderType: typeFilter.value })
    if (res.data.code === 200) { tableData.value = res.data.data.records; total.value = res.data.data.total }
  } finally { loading.value = false }
}

const viewDetail = async (row) => {
  const res = await getWorkOrderDetail(row.id)
  if (res.data.code === 200) { detail.value = res.data.data; detailVisible.value = true }
  else ElMessage.error(res.data.message)
}

const showAssign = async (row) => {
  assignForm.value = { orderId: row.id, assignedTo: null }
  const res = await getGuards()
  if (res.data.code === 200) users.value = res.data.data
  assignVisible.value = true
}

const handleAssign = async () => {
  const res = await assignWorkOrder(assignForm.value)
  if (res.data.code === 200) { ElMessage.success('指派成功'); assignVisible.value = false; fetchData() }
  else ElMessage.error(res.data.message)
}

const showDispose = (row) => {
  disposeForm.value = { orderId: row.id, disposalDesc: '', disposalImages: '' }
  disposeVisible.value = true
}

const handleDispose = async () => {
  const res = await disposeWorkOrder(disposeForm.value)
  if (res.data.code === 200) { ElMessage.success('处置提交成功'); disposeVisible.value = false; fetchData() }
  else ElMessage.error(res.data.message)
}

const showReview = (row) => {
  reviewForm.value = { orderId: row.id, reviewResult: 1, reviewComment: '' }
  reviewVisible.value = true
}

const handleReview = async () => {
  const res = await reviewWorkOrder(reviewForm.value)
  if (res.data.code === 200) { ElMessage.success('审核完成'); reviewVisible.value = false; fetchData() }
  else ElMessage.error(res.data.message)
}

const handleExport = async () => {
  const res = await exportWorkOrders()
  if (res.data.code === 200) ElMessage.success('导出成功，共' + res.data.data.length + '条')
  else ElMessage.error(res.data.message)
}

onMounted(fetchData)
</script>

<style scoped>
.workorder-container { padding: 20px; }
</style>
