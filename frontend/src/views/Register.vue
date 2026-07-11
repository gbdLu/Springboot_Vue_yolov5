<template>
  <div class="register-container">
    <div class="register-box">
      <h2>林区明火智能监管系统</h2>
      <h3>用户注册</h3>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="选择角色" style="width:100%">
            <el-option label="巡护员" value="GUARD" />
            <el-option label="林区管理员" value="MANAGER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" style="width:100%">
            注 册
          </el-button>
        </el-form-item>
        <el-form-item>
          <div style="text-align:center;width:100%">
            已有账号？<el-link type="primary" @click="router.push('/login')">去登录</el-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  roleCode: 'GUARD'
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await request.post('/auth/register', {
        username: form.value.username,
        password: form.value.password,
        realName: form.value.realName,
        phone: form.value.phone,
        roleCode: form.value.roleCode
      })
      if (res.data.code === 200) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(res.data.message)
      }
    } catch (e) {
      ElMessage.error('注册失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1a3a2a;
}
.register-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  width: 420px;
}
.register-box h2 {
  text-align: center;
  margin-bottom: 10px;
  color: #1a3a2a;
}
.register-box h3 {
  text-align: center;
  margin-bottom: 30px;
  color: #666;
  font-weight: normal;
}
</style>
