import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const setToken = (t) => {
    token.value = t
    localStorage.setItem('token', t)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('user', JSON.stringify(info))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
