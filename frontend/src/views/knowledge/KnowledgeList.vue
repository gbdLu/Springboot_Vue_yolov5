<template>
  <div class="knowledge-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>📚 防火知识库</span>
          <div>
            <el-select v-model="category" placeholder="分类筛选" clearable style="width:150px;margin-right:10px" @change="fetchData">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-input v-model="keyword" placeholder="搜索" style="width:200px;margin-right:10px" clearable @clear="fetchData" @keyup.enter="fetchData" />
            <el-button v-if="isAdmin" type="primary" @click="showAddDialog">发布文章</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-for="item in tableData" :key="item.id" style="margin-bottom:20px">
          <el-card shadow="hover" @click="viewDetail(item)" style="cursor:pointer">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>{{ item.title }}</span>
                <el-tag size="small">{{ item.category }}</el-tag>
              </div>
            </template>
            <div style="color:#666;font-size:13px;height:60px;overflow:hidden">{{ item.content?.substring(0, 100) }}...</div>
            <div style="margin-top:10px;color:#999;font-size:12px">
              <span>👁 {{ item.viewCount || 0 }}</span>
              <span style="margin-left:15px">{{ item.creatorName }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-pagination
        v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        layout="total, prev, pager, next" style="margin-top:15px;justify-content:flex-end" @current-change="fetchData"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" :title="current.title" width="700px">
      <div style="margin-bottom:10px;color:#999">
        <el-tag>{{ current.category }}</el-tag>
        <span style="margin-left:15px">👁 {{ current.viewCount }}</span>
        <span style="margin-left:15px">{{ current.creatorName }}</span>
      </div>
      <div style="white-space:pre-wrap;line-height:1.8">{{ current.content }}</div>
      <template #footer v-if="isAdmin">
        <el-button @click="showEditDialog(current)">编辑</el-button>
        <el-button type="danger" @click="handleDelete(current)">删除</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" :title="isEdit ? '编辑文章' : '发布文章'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" allow-create filterable>
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="10" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKnowledgeList, getKnowledgeDetail, addKnowledge, updateKnowledge, deleteKnowledge, getCategories } from '@/api/knowledge'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const keyword = ref('')
const category = ref(null)
const categories = ref([])

const detailVisible = ref(false)
const editVisible = ref(false)
const isEdit = ref(false)
const current = ref({})
const form = ref({})

const isAdmin = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.roleCode === 'ADMIN'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value, category: category.value })
    if (res.data.code === 200) { tableData.value = res.data.data.records; total.value = res.data.data.total }
  } finally { loading.value = false }
}

const fetchCategories = async () => {
  const res = await getCategories()
  if (res.data.code === 200) categories.value = res.data.data
}

const viewDetail = async (item) => {
  const res = await getKnowledgeDetail(item.id)
  if (res.data.code === 200) { current.value = res.data.data; detailVisible.value = true }
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = { category: '防火知识' }
  editVisible.value = true
}

const showEditDialog = (item) => {
  isEdit.value = true
  form.value = { ...item }
  detailVisible.value = false
  editVisible.value = true
}

const handleSubmit = async () => {
  const api = isEdit.value ? updateKnowledge : addKnowledge
  const res = await api(form.value)
  if (res.data.code === 200) { ElMessage.success(res.data.message); editVisible.value = false; fetchData(); fetchCategories() }
  else ElMessage.error(res.data.message)
}

const handleDelete = (item) => {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    const res = await deleteKnowledge(item.id)
    if (res.data.code === 200) { ElMessage.success('删除成功'); detailVisible.value = false; fetchData() }
  })
}

onMounted(() => { fetchData(); fetchCategories() })
</script>

<style scoped>
.knowledge-container { padding: 20px; }
</style>
