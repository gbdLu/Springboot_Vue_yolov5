<template>
  <div class="canvas-overlay-container">
    <canvas ref="canvasRef" :width="width" :height="height" class="detection-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'

const props = defineProps({
  imageUrl: { type: String, default: '' },
  detections: { type: Array, default: () => [] },
  width: { type: Number, default: 800 },
  height: { type: Number, default: 600 }
})

const canvasRef = ref(null)

const COLORS = {
  fire: '#FF0000',
  human: '#00FF00',
  smoke: '#FFA500'
}

const LABELS = {
  fire: '🔥 明火',
  human: '👤 人员',
  smoke: '💨 烟雾'
}

const draw = () => {
  if (!canvasRef.value || !props.imageUrl) return
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    canvas.width = props.width
    canvas.height = props.height
    const scaleX = props.width / img.width
    const scaleY = props.height / img.height
    ctx.drawImage(img, 0, 0, props.width, props.height)

    props.detections.forEach(det => {
      const [x1, y1, x2, y2] = det.bbox
      const x = x1 * scaleX, y = y1 * scaleY
      const w = (x2 - x1) * scaleX, h = (y2 - y1) * scaleY
      const color = COLORS[det.class_name] || '#FF0000'
      const label = LABELS[det.class_name] || det.class_name
      const confidence = ((det.confidence || 0) * 100).toFixed(1)

      // 绘制边框
      ctx.strokeStyle = color
      ctx.lineWidth = 3
      ctx.strokeRect(x, y, w, h)

      // 绘制标签背景
      const text = `${label} ${confidence}%`
      ctx.font = 'bold 14px Arial'
      const textWidth = ctx.measureText(text).width
      ctx.fillStyle = color
      ctx.fillRect(x, y - 28, textWidth + 12, 28)

      // 绘制标签文字
      ctx.fillStyle = '#FFFFFF'
      ctx.fillText(text, x + 6, y - 8)
    })
  }
  img.src = props.imageUrl
}

watch(() => [props.imageUrl, props.detections], draw, { deep: true })
onMounted(draw)
</script>

<style scoped>
.canvas-overlay-container {
  display: inline-block;
}
.detection-canvas {
  border: 2px solid #eee;
  border-radius: 8px;
  max-width: 100%;
}
</style>
