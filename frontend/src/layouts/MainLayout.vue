<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <div class="logo-mark">
          <el-icon size="22"><ChatDotRound /></el-icon>
        </div>
        <div v-show="!isCollapse" class="logo-text">
          <strong>校园知识平台</strong>
        </div>
      </div>

      <div v-show="!isCollapse" class="nav-section-label">系统导航</div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        class="layout-menu"
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
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <div class="page-meta">
            <strong>{{ currentPageTitle }}</strong>
          </div>
        </div>

        <div class="header-right">
          <div class="user-panel">
            <div class="user-panel__avatar">{{ userInitial }}</div>
            <div class="user-panel__info">
              <strong>{{ userStore.username }}</strong>
              <span>{{ roleLabel }}</span>
            </div>
          </div>
          <el-button class="logout-btn" @click="userStore.logout()">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <div class="layout-main__inner">
          <router-view v-slot="{ Component }">
            <keep-alive include="ChatView">
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </div>
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

const userInitial = computed(() => userStore.username?.slice(0, 1).toUpperCase() || 'U')

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

const currentPageTitle = computed(() => {
  const current = allMenus.find((item) => route.path.startsWith(item.path))
  return current?.title || '工作台'
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(72, 149, 255, 0.08), transparent 20%),
    linear-gradient(180deg, #f4f8fc 0%, #eef3f8 100%);
}

.layout-container > .el-container {
  min-width: 0;
  min-height: 0;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  padding: 18px 12px;
  background: linear-gradient(180deg, #0b1f39 0%, #112a47 58%, #123553 100%);
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 10px 10px 18px;
  color: #fff;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(173, 221, 255, 0.24);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(37, 110, 176, 0.92), rgba(25, 147, 167, 0.9));
  box-shadow: 0 12px 24px rgba(3, 16, 37, 0.25);
}

.logo-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo-text strong {
  color: #f5fbff;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.nav-section-label {
  padding: 0 12px 10px;
  color: rgba(205, 224, 240, 0.58);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 76px;
  margin: 16px 16px 0;
  padding: 0 22px;
  border: 1px solid rgba(193, 210, 227, 0.72);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 12px 32px rgba(17, 41, 71, 0.08);
  backdrop-filter: blur(16px);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 16px;
}

.header-right {
  gap: 14px;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #edf4fb;
  color: #17385b;
  cursor: pointer;
  font-size: 18px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.collapse-btn:hover {
  background: #e1edf8;
  transform: translateY(-1px);
}

.page-meta {
  display: flex;
  flex-direction: column;
}

.page-meta strong {
  color: #17324d;
  font-size: 22px;
  line-height: 1.2;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border: 1px solid rgba(193, 210, 227, 0.84);
  border-radius: 999px;
  background: #f8fbfe;
}

.user-panel__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1166aa, #1a9bb1);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.user-panel__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-panel__info strong {
  color: #19314b;
  font-size: 14px;
}

.user-panel__info span {
  color: #74879b;
  font-size: 12px;
}

.logout-btn {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(201, 214, 228, 0.92);
  border-radius: 12px;
  background: #fff;
  color: #274563;
}

.layout-main {
  display: flex;
  min-height: 0;
  padding: 16px;
  background: transparent;
  overflow: hidden;
}

.layout-main__inner {
  flex: 1;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 48px;
  margin-bottom: 6px;
  border-radius: 14px;
  color: #c6d7e8;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #f3fbff;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(23, 104, 177, 0.9), rgba(22, 142, 165, 0.88));
  color: #ffffff;
  box-shadow: 0 14px 30px rgba(4, 17, 35, 0.26);
}

:deep(.el-menu-item .el-icon) {
  color: inherit;
}

@media (max-width: 900px) {
  .layout-header {
    height: auto;
    padding: 16px;
  }

  .page-meta strong {
    font-size: 18px;
  }

  .logout-btn {
    padding: 0 12px;
  }
}

@media (max-width: 640px) {
  .layout-aside {
    padding: 14px 8px;
  }

  .layout-header {
    margin: 12px 12px 0;
    padding: 14px;
    border-radius: 18px;
  }

  .header-right {
    gap: 10px;
  }

  .user-panel {
    padding-right: 8px;
  }

  .user-panel__info {
    display: none;
  }

  .layout-main {
    padding: 12px;
  }
}
</style>
