<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <el-icon size="24"><ChatDotRound /></el-icon>
        <span v-show="!isCollapse">校园问答助手</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Fold v-if="!isCollapse" /><Expand v-else />
        </el-icon>
        <div class="header-right">
          <span class="user-info">{{ userStore.username }} ({{ roleLabel }})</span>
          <el-button type="danger" text @click="userStore.logout()">退出</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive include="ChatView">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/types'

const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

const roleLabel = computed(() => {
  const map: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[userStore.role] || ''
})

interface MenuItem { path: string; title: string; icon: string; roles?: Role[] }

const allMenus: MenuItem[] = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/chat', title: 'AI 对话', icon: 'ChatDotRound' },
  { path: '/users', title: '用户管理', icon: 'User', roles: ['ADMIN'] },
  { path: '/teachers', title: '教师信息', icon: 'Avatar', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/students', title: '学生信息', icon: 'Reading', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/courses', title: '课程信息', icon: 'Notebook', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/grades', title: '成绩信息', icon: 'TrendCharts', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/rag', title: '知识库', icon: 'UploadFilled', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
]

const menuItems = computed(() =>
  allMenus.filter((m) => !m.roles || m.roles.includes(userStore.role as Role))
)
</script>

<style scoped>
.layout-container { min-height: 100vh; }
.layout-aside {
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}
.collapse-btn { cursor: pointer; font-size: 20px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-info { color: #606266; font-size: 14px; }
.layout-main { background: #f0f2f5; }
.el-menu { border-right: none; }
</style>
