<template>
  <div class="upload-container">
    <el-card>
      <template #header>🔥 AI 智能识别</template>
      <el-form>
        <el-form-item label="所属林区">
          <el-select v-model="forestAreaId" placeholder="请选择林区">
            <el-option
              v-for="item in forestAreas"
              :key="item.id"
              :label="item.areaName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            drag
            :auto-upload="false"
            :file-list="fileList"
            :on-change="handleFileChange"
            multiple
            accept="image/*"
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
        <span>📊 识别结果</span>
        <el-tag :type="resultTotal > 0 ? 'danger' : 'success'">
          {{ resultTotal > 0 ? `发现 ${resultTotal} 处隐患` : '未发现隐患' }}
        </el-tag>
      </template>
      <div class="stats">
        <el-statistic title="🔥 明火" :value="result.fireCount || 0" />
        <el-statistic title="👤 人员" :value="result.humanCount || 0" />
        <el-statistic title="💨 烟雾" :value="result.smokeCount || 0" />
        <el-statistic title="⚠️ 违规用火" :value="result.firePeopleCount || 0" />
      </div>
      <canvas ref="canvasRef" class="canvas-preview"></canvas>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const forestAreaId = ref(null)
const fileList = ref([])
const canvasRef = ref(null)
const uploading = ref(false)
const resultVisible = ref(false)
const resultTotal = ref(0)
const forestAreas = ref([])
const result = ref({ fireCount: 0, humanCount: 0, smokeCount: 0, firePeopleCount: 0, detections: [] })

const handleFileChange = (file) => { fileList.value = [file] }

const drawDetections = (imageUrl, detections) => {
  nextTick(() => {
    const canvas = canvasRef.value
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    const img = new Image()
    img.onload = () => {
      canvas.width = 800
      canvas.height = 600
      ctx.drawImage(img, 0, 0, 800, 600)
      const scaleX = 800 / img.width
      const scaleY = 600 / img.height
      const colors = { fire: '#FF0000', human: '#00FF00', smoke: '#FFA500' }
      detections.forEach(det => {
        const [x1, y1, x2, y2] = det.bbox
        const x = x1 * scaleX, y = y1 * scaleY, w = (x2 - x1) * scaleX, h = (y2 - y1) * scaleY
        const color = colors[det.class_name] || '#FF0000'
        ctx.strokeStyle = color
        ctx.lineWidth = 2
        ctx.strokeRect(x, y, w, h)
        ctx.fillStyle = color
        ctx.fillRect(x, y - 22, 80, 22)
        ctx.fillStyle = '#fff'
        ctx.font = '12px Arial'
        ctx.fillText(`${det.class_name} ${(det.confidence*100).toFixed(1)}%`, x + 4, y - 5)
      })
    }
    img.src = imageUrl
  })
}

const submitUpload = async () => {
  if (!fileList.value.length) { ElMessage.warning('请选择图片'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0].raw)
    if (forestAreaId.value) formData.append('forestAreaId', forestAreaId.value)
    const res = await request.post('/detection/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
    if (res.data.code === 200) {
      const data = res.data.data
      result.value = data
      resultTotal.value = data.totalCount || 0
      resultVisible.value = true
      drawDetections(`/api/detection/image/${data.recordId}`, data.detections || [])
      ElMessage.success(`识别完成，发现 ${data.totalCount} 个目标`)
    }
  } catch (e) { ElMessage.error('识别失败') } finally { uploading.value = false }
}
</script>

<style scoped>
.upload-container { padding: 20px; }
.stats { display: flex; gap: 30px; margin-bottom: 20px; }
.canvas-preview { width: 100%; max-width: 800px; border: 1px solid #ddd; border-radius: 8px; }
.result-card { margin-top: 20px; }
</style>