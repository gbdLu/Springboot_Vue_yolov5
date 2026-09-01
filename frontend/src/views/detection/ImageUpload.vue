<template>
  <div class="upload-container">
    <el-card>
      <template #header>🔥 AI 智能识别</template>
      <el-form>
        <el-form-item label="所属林区">
          <el-select v-model="forestAreaId" placeholder="请选择林区">
            <el-option v-for="item in forestAreas" :key="item.id" :label="item.areaName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <div style="display:flex;gap:10px;align-items:flex-start">
            <el-upload
              drag
              :auto-upload="false"
              :file-list="fileList"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              multiple
              accept="image/jpeg,image/png,image/bmp,image/webp"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖拽图片到此处，或 <em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">支持多选，可批量上传识别</div>
              </template>
            </el-upload>
            <div style="display:flex;flex-direction:column;gap:8px">
              <el-button @click="selectFolder">
                <el-icon><FolderOpened /></el-icon>导入文件夹
              </el-button>
              <input ref="folderInput" type="file" webkitdirectory style="display:none" @change="handleFolderSelect" />
              <el-button v-if="fileList.length > 0" @click="clearFiles">
                <el-icon><Delete /></el-icon>清空列表
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitUpload" :loading="uploading">
            {{ uploading ? `识别中 (${currentIndex}/${fileList.length})` : '开始识别' }}
          </el-button>
          <el-button v-if="fileList.length > 1" @click="submitBatchUpload" :loading="batchUploading">
            批量识别 ({{ fileList.length }}张)
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 进度条 -->
    <el-card v-if="batchUploading" class="progress-card">
      <template #header>📊 识别进度</template>
      <el-progress :percentage="progressPercent" :format="progressFormat" />
      <div style="margin-top:10px;color:#666">
        当前第 {{ currentIndex }} 张 / 共 {{ fileList.length }} 张
      </div>
    </el-card>

    <!-- 单张识别结果 -->
    <el-card v-if="resultVisible" class="result-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>📊 识别结果</span>
          <el-tag :type="resultTotal > 0 ? 'danger' : 'success'" size="large">
            {{ resultTotal > 0 ? `发现 ${resultTotal} 处隐患` : '未发现隐患' }}
          </el-tag>
        </div>
      </template>
      <div class="stats">
        <el-statistic title="🔥 明火" :value="result.fireCount || 0" />
        <el-statistic title="👤 人员" :value="result.humanCount || 0" />
        <el-statistic title="💨 烟雾" :value="result.smokeCount || 0" />
        <el-statistic title="⚠️ 违规用火" :value="result.firePeopleCount || 0" />
      </div>
      <div v-if="result.resultImage" style="margin-top:15px">
        <img :src="result.resultImage" style="max-width:100%;border-radius:8px;border:2px solid #eee" />
      </div>
      <CanvasOverlay
        v-else-if="result.recordId"
        :imageUrl="`/api/detection/image/${result.recordId}`"
        :detections="result.detections || []"
        :width="800"
        :height="600"
      />
      <div v-if="result.workOrderIds && result.workOrderIds.length > 0" style="margin-top:15px">
        <el-alert type="warning" :closable="false" title="已自动生成工单" :description="'工单ID: ' + result.workOrderIds.join(', ')" />
      </div>
    </el-card>

    <!-- 批量识别结果汇总 -->
    <el-card v-if="batchResults.length > 0" class="result-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>📋 批量识别结果汇总</span>
          <el-tag :type="batchHazardCount > 0 ? 'danger' : 'success'" size="large">
            {{ batchHazardCount > 0 ? `${batchHazardCount} 个文件有隐患` : '全部安全' }}
          </el-tag>
        </div>
      </template>
      <el-table :data="batchResults" stripe>
        <el-table-column label="序号" width="60">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="文件名">
          <template #default="{ row }">
            <el-link v-if="row.success && row.recordId" type="primary" @click="viewBatchDetail(row)">
              {{ row.fileName }}
            </el-link>
            <span v-else>{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="识别数量" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.success" :type="row.totalCount > 0 ? 'danger' : 'success'">
              {{ row.totalCount || 0 }}
            </el-tag>
            <el-tag v-else type="info">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否有隐患" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.success" :type="row.hasHazard ? 'danger' : 'success'">
              {{ row.hasHazard ? '有' : '无' }}
            </el-tag>
            <el-tag v-else type="info">-</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="200">
          <template #default="{ row }">
            <template v-if="row.success">
              <el-tag type="danger" size="small" style="margin-right:5px">🔥 {{ row.fireCount || 0 }}</el-tag>
              <el-tag type="success" size="small" style="margin-right:5px">👤 {{ row.humanCount || 0 }}</el-tag>
              <el-tag type="warning" size="small">💨 {{ row.smokeCount || 0 }}</el-tag>
            </template>
            <span v-else style="color:#999">{{ row.message }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批量结果详情弹窗 -->
    <el-dialog v-model="batchDetailVisible" title="识别结果详情" width="800px">
      <div class="detail-stats">
        <el-statistic title="🔥 明火" :value="batchDetail.fireCount || 0" />
        <el-statistic title="👤 人员" :value="batchDetail.humanCount || 0" />
        <el-statistic title="💨 烟雾" :value="batchDetail.smokeCount || 0" />
      </div>
      <div v-if="batchDetail.recordId" style="margin-top:15px">
        <h4>识别结果图：</h4>
        <img
          v-if="batchDetail.resultImage"
          :src="batchDetail.resultImage"
          style="max-width:100%;border-radius:8px;border:2px solid #eee"
        />
        <img
          v-else
          :src="`/api/detection/image/${batchDetail.recordId}?type=result`"
          style="max-width:100%;border-radius:8px;border:2px solid #eee"
          @error="(e) => e.target.src = `/api/detection/image/${batchDetail.recordId}`"
        />
      </div>
      <div v-if="batchDetail.detections && batchDetail.detections.length > 0" style="margin-top:15px">
        <h4>检测目标详情：</h4>
        <el-tag
          v-for="(det, i) in batchDetail.detections"
          :key="i"
          :type="det.class_name === 'fire' ? 'danger' : det.class_name === 'smoke' ? 'warning' : 'success'"
          style="margin:3px"
        >
          {{ det.class_name }} {{ (det.confidence * 100).toFixed(1) }}%
        </el-tag>
      </div>
      <div v-if="batchDetail.workOrderIds && batchDetail.workOrderIds.length > 0" style="margin-top:15px">
        <el-alert type="warning" :closable="false" title="已自动生成工单" :description="'工单ID: ' + batchDetail.workOrderIds.join(', ')" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, FolderOpened, Delete } from '@element-plus/icons-vue'
import { getForestList } from '@/api/forest'
import { compressImage } from '@/utils/imageCompress'
import CanvasOverlay from '@/components/CanvasOverlay.vue'
import request from '@/utils/request'

const forestAreaId = ref(null)
const fileList = ref([])
const uploading = ref(false)
const batchUploading = ref(false)
const currentIndex = ref(0)
const resultVisible = ref(false)
const resultTotal = ref(0)
const forestAreas = ref([])
const result = ref({ fireCount: 0, humanCount: 0, smokeCount: 0, firePeopleCount: 0, detections: [], recordId: null, workOrderIds: [] })
const batchResults = ref([])
const folderInput = ref(null)

// 批量结果详情弹窗
const batchDetailVisible = ref(false)
const batchDetail = ref({})

const progressPercent = computed(() => {
  if (fileList.value.length === 0) return 0
  return Math.round((currentIndex.value / fileList.value.length) * 100)
})

const batchHazardCount = computed(() => {
  return batchResults.value.filter(r => r.success && r.hasHazard).length
})

const progressFormat = (percentage) => {
  return percentage === 100 ? '完成' : `${percentage}%`
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

// 选择文件夹
const selectFolder = () => {
  folderInput.value.click()
}

// 处理文件夹选择
const handleFolderSelect = (e) => {
  const files = Array.from(e.target.files)
  const imageFiles = files.filter(f => f.type.startsWith('image/'))
  if (imageFiles.length === 0) {
    ElMessage.warning('文件夹中没有找到图片文件')
    return
  }
  // 添加到文件列表
  const newFiles = imageFiles.map(f => ({
    name: f.name,
    raw: f,
    size: f.size,
    status: 'ready'
  }))
  fileList.value = [...fileList.value, ...newFiles]
  ElMessage.success(`已导入 ${imageFiles.length} 张图片`)
  // 清空input以允许重新选择同一文件夹
  e.target.value = ''
}

// 清空文件列表
const clearFiles = () => {
  fileList.value = []
  resultVisible.value = false
  batchResults.value = []
  ElMessage.info('已清空文件列表')
}

// 单张识别
const submitUpload = async () => {
  if (!fileList.value.length) { ElMessage.warning('请选择图片'); return }
  uploading.value = true
  resultVisible.value = false
  batchResults.value = []
  try {
    const compressed = await compressImage(fileList.value[0].raw, 1920, 0.85)
    const formData = new FormData()
    formData.append('file', compressed)
    if (forestAreaId.value) formData.append('forestAreaId', forestAreaId.value)

    const res = await request.post('/detection/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })

    if (res.data.code === 200) {
      const data = res.data.data
      result.value = data
      resultTotal.value = data.totalCount || 0
      resultVisible.value = true
      ElMessage.success(`识别完成，发现 ${data.totalCount} 个目标`)
    } else {
      ElMessage.error(res.data.message || '识别失败')
    }
  } catch (e) {
    ElMessage.error('识别失败：' + (e.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

// 批量识别
const submitBatchUpload = async () => {
  if (!fileList.value.length) { ElMessage.warning('请选择图片'); return }
  batchUploading.value = true
  resultVisible.value = false
  batchResults.value = []
  currentIndex.value = 0

  try {
    // 压缩所有图片
    const compressedFiles = []
    for (const file of fileList.value) {
      const compressed = await compressImage(file.raw, 1920, 0.85)
      compressedFiles.push(compressed)
    }

    const formData = new FormData()
    compressedFiles.forEach(file => {
      formData.append('files', file)
    })
    if (forestAreaId.value) formData.append('forestAreaId', forestAreaId.value)

    // 模拟进度（因为后端是同步处理）
    const progressInterval = setInterval(() => {
      if (currentIndex.value < fileList.value.length) {
        currentIndex.value++
      }
    }, 2000)

    const res = await request.post('/detection/batch-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000
    })

    clearInterval(progressInterval)
    currentIndex.value = fileList.value.length

    if (res.data.code === 200) {
      batchResults.value = res.data.data
      const hazardCount = batchResults.value.filter(r => r.success && r.hasHazard).length
      ElMessage.success(`批量识别完成，${hazardCount} 个文件发现隐患`)
    } else {
      ElMessage.error(res.data.message || '批量识别失败')
    }
  } catch (e) {
    ElMessage.error('批量识别失败：' + (e.message || '未知错误'))
  } finally {
    batchUploading.value = false
  }
}

// 查看批量结果详情
const viewBatchDetail = async (row) => {
  try {
    // 先获取记录详情
    const res = await request.get(`/detection/detail/${row.recordId}`)
    if (res.data.code === 200) {
      batchDetail.value = {
        ...res.data.data,
        detections: res.data.data.resultJson ? JSON.parse(res.data.data.resultJson).detections || [] : [],
        workOrderIds: row.workOrderIds || []
      }
      // 如果有结果图base64
      if (row.resultImage) {
        batchDetail.value.resultImage = row.resultImage
      }
      batchDetailVisible.value = true
    } else {
      ElMessage.error('获取详情失败')
    }
  } catch (e) {
    ElMessage.error('获取详情失败：' + e.message)
  }
}

onMounted(async () => {
  const res = await getForestList({ pageNum: 1, pageSize: 100 })
  if (res.data.code === 200) forestAreas.value = res.data.data.records
})
</script>

<style scoped>
.upload-container { padding: 20px; }
.stats { display: flex; gap: 30px; margin-bottom: 20px; }
.detail-stats { display: flex; gap: 30px; margin-bottom: 20px; }
.result-card { margin-top: 20px; }
.progress-card { margin-top: 20px; }
</style>
