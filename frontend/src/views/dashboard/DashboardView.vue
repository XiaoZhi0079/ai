<template>
  <div class="dashboard">
    <h2>欢迎使用校园知识问答助手</h2>
    <p class="welcome-text">你好，{{ userStore.username }}！当前角色：{{ roleLabel }}</p>
    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :span="8" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card" @click="router.push(card.path)">
          <div class="stat-card-inner">
            <el-icon :size="40" :color="card.color"><component :is="card.icon" /></el-icon>
            <div>
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-desc">{{ card.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const roleLabel = computed(() => {
  const map: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[userStore.role] || ''
})

const cards = computed(() => {
  const base = [
    { title: 'AI 对话', desc: '智能问答助手', icon: 'ChatDotRound', color: '#409eff', path: '/chat' }
  ]
  if (userStore.isAdmin || userStore.isTeacher) {
    base.push(
      { title: '学生管理', desc: '管理学生信息', icon: 'Reading', color: '#67c23a', path: '/students' },
      { title: '课程管理', desc: '管理课程信息', icon: 'Notebook', color: '#e6a23c', path: '/courses' }
    )
  }
  return base
})
</script>

<style scoped>
.dashboard { padding: 20px; }
.welcome-text { color: #909399; margin-top: 8px; }
.stat-card { cursor: pointer; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-4px); }
.stat-card-inner { display: flex; align-items: center; gap: 16px; }
.stat-title { font-size: 18px; font-weight: 600; }
.stat-desc { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
