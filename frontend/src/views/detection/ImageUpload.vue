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
          <el-upload
            drag
            :auto-upload="false"
            :file-list="fileList"
            :on-change="handleFileChange"
            multiple
            accept="image/jpeg,image/png,image/bmp,image/webp"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽图片到此处，或 <em>点击上传</em></div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitUpload" :loading="uploading">开始识别</el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
      <!-- 结果图（FastAPI 返回的带标注图片） -->
      <div v-if="result.resultImage" style="margin-top:15px">
        <img :src="result.resultImage" style="max-width:100%;border-radius:8px;border:2px solid #eee" />
      </div>
      <!-- 无结果图时回退到原图 + Canvas 标注 -->
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getForestList } from '@/api/forest'
import { compressImage } from '@/utils/imageCompress'
import CanvasOverlay from '@/components/CanvasOverlay.vue'
import request from '@/utils/request'

const forestAreaId = ref(null)
const fileList = ref([])
const uploading = ref(false)
const resultVisible = ref(false)
const resultTotal = ref(0)
const forestAreas = ref([])
const result = ref({ fireCount: 0, humanCount: 0, smokeCount: 0, firePeopleCount: 0, detections: [], recordId: null, workOrderIds: [] })

const handleFileChange = (file) => { fileList.value = [file] }

const submitUpload = async () => {
  if (!fileList.value.length) { ElMessage.warning('请选择图片'); return }
  uploading.value = true
  try {
    // 压缩图片
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

onMounted(async () => {
  const res = await getForestList({ pageNum: 1, pageSize: 100 })
  if (res.data.code === 200) forestAreas.value = res.data.data.records
})
</script>

<style scoped>
.upload-container { padding: 20px; }
.stats { display: flex; gap: 30px; margin-bottom: 20px; }
.result-card { margin-top: 20px; }
</style>
