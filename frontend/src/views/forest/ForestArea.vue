<template>
  <div class="forest-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>🌲 林区管理</span>
          <div>
            <el-input v-model="keyword" placeholder="搜索林区" style="width:200px;margin-right:10px" clearable @clear="fetchData" @keyup.enter="fetchData" />
            <el-select v-model="riskLevel" placeholder="火险等级" clearable style="width:120px;margin-right:10px" @change="fetchData">
              <el-option label="低" :value="1" />
              <el-option label="中" :value="2" />
              <el-option label="高" :value="3" />
            </el-select>
            <el-button type="primary" @click="showAddDialog">新增林区</el-button>
            <el-button @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="areaName" label="林区名称" />
        <el-table-column prop="location" label="位置" />
        <el-table-column label="火险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.fireRiskLevel === 3 ? 'danger' : row.fireRiskLevel === 2 ? 'warning' : 'success'">
              {{ ['', '低', '中', '高'][row.fireRiskLevel] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerName" label="负责人" width="100" />
        <el-table-column prop="totalArea" label="面积(亩)" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" @click="showPoints(row)">监控点</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑林区' : '新增林区'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="林区名称"><el-input v-model="form.areaName" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="火险等级">
          <el-select v-model="form.fireRiskLevel">
            <el-option label="低" :value="1" /><el-option label="中" :value="2" /><el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="面积(亩)"><el-input-number v-model="form.totalArea" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 监控点对话框 -->
    <el-dialog v-model="pointDialogVisible" title="监控点管理" width="700px">
      <div style="margin-bottom:15px">
        <el-button type="primary" size="small" @click="showAddPoint">新增监控点</el-button>
      </div>
      <el-table :data="points" stripe>
        <el-table-column prop="pointName" label="名称" />
        <el-table-column prop="pointType" label="类型" />
        <el-table-column prop="longitude" label="经度" />
        <el-table-column prop="latitude" label="纬度" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="editPoint(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePoint(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 监控点编辑对话框 -->
    <el-dialog v-model="pointEditVisible" :title="isEditPoint ? '编辑监控点' : '新增监控点'" width="400px">
      <el-form :model="pointForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="pointForm.pointName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="pointForm.pointType">
            <el-option label="摄像头" value="camera" /><el-option label="传感器" value="sensor" />
          </el-select>
        </el-form-item>
        <el-form-item label="经度"><el-input-number v-model="pointForm.longitude" :precision="6" :step="0.001" /></el-form-item>
        <el-form-item label="纬度"><el-input-number v-model="pointForm.latitude" :precision="6" :step="0.001" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="pointForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointEditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPoint">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getForestList, addForest, updateForest, deleteForest, exportForest, getPoints, addPoint, updatePoint, deletePoint as apiDeletePoint } from '@/api/forest'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const riskLevel = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})

const pointDialogVisible = ref(false)
const pointEditVisible = ref(false)
const isEditPoint = ref(false)
const currentAreaId = ref(null)
const points = ref([])
const pointForm = ref({})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getForestList({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value, fireRiskLevel: riskLevel.value })
    if (res.data.code === 200) {
      tableData.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally { loading.value = false }
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = { fireRiskLevel: 1, status: 1 }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const api = isEdit.value ? updateForest : addForest
  const res = await api(form.value)
  if (res.data.code === 200) {
    ElMessage.success(res.data.message)
    dialogVisible.value = false
    fetchData()
  } else ElMessage.error(res.data.message)
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该林区？', '提示', { type: 'warning' }).then(async () => {
    const res = await deleteForest(row.id)
    if (res.data.code === 200) { ElMessage.success('删除成功'); fetchData() }
  })
}

const handleExport = async () => {
  const res = await exportForest()
  if (res.data.code === 200) {
    ElMessage.success('导出成功，共' + res.data.data.length + '条')
  }
}

const showPoints = async (row) => {
  currentAreaId.value = row.id
  pointDialogVisible.value = true
  const res = await getPoints(row.id)
  points.value = res.data.code === 200 ? res.data.data : []
}

const showAddPoint = () => {
  isEditPoint.value = false
  pointForm.value = { forestAreaId: currentAreaId.value, pointType: 'camera' }
  pointEditVisible.value = true
}

const editPoint = (row) => {
  isEditPoint.value = true
  pointForm.value = { ...row }
  pointEditVisible.value = true
}

const submitPoint = async () => {
  const api = isEditPoint.value ? updatePoint : addPoint
  const res = await api(pointForm.value)
  if (res.data.code === 200) {
    ElMessage.success(res.data.message)
    pointEditVisible.value = false
    const r = await getPoints(currentAreaId.value)
    points.value = r.data.code === 200 ? r.data.data : []
  }
}

const deletePoint = async (row) => {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    await apiDeletePoint(row.id)
    const r = await getPoints(currentAreaId.value)
    points.value = r.data.code === 200 ? r.data.data : []
  })
}

onMounted(fetchData)
</script>

<style scoped>
.forest-container { padding: 20px; }
</style>
