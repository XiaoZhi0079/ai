import { createRouter, createWebHistory } from 'vue-router'
import type { Role } from '@/types'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘', icon: 'Odometer' }
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/chat/ChatView.vue'),
          meta: { title: 'AI 对话', icon: 'ChatDotRound' }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/user/UserView.vue'),
          meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] as Role[] }
        },
        {
          path: 'teachers',
          name: 'Teachers',
          component: () => import('@/views/teacher/TeacherView.vue'),
          meta: { title: '教师信息', icon: 'Avatar', roles: ['ADMIN', 'TEACHER', 'STUDENT'] as Role[] }
        },
        {
          path: 'students',
          name: 'Students',
          component: () => import('@/views/student/StudentView.vue'),
          meta: { title: '学生信息', icon: 'Reading', roles: ['ADMIN', 'TEACHER', 'STUDENT'] as Role[] }
        },
        {
          path: 'courses',
          name: 'Courses',
          component: () => import('@/views/course/CourseView.vue'),
          meta: { title: '课程信息', icon: 'Notebook', roles: ['ADMIN', 'TEACHER', 'STUDENT'] as Role[] }
        },
        {
          path: 'grades',
          name: 'Grades',
          component: () => import('@/views/grade/GradeView.vue'),
          meta: { title: '成绩信息', icon: 'TrendCharts', roles: ['ADMIN', 'TEACHER', 'STUDENT'] as Role[] }
        },
        {
          path: 'rag',
          name: 'Rag',
          component: () => import('@/views/rag/RagView.vue'),
          meta: { title: '知识库', icon: 'UploadFilled', roles: ['ADMIN', 'TEACHER', 'STUDENT'] as Role[] }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach((to, _from, next) => {
  let isLoggedIn = ''
  let userRole = ''
  try {
    const auth = localStorage.getItem('user_auth')
    if (auth) {
      const parsed = JSON.parse(auth)
      isLoggedIn = parsed.token || ''
      userRole = parsed.role || ''
    }
  } catch {
    localStorage.removeItem('user_auth')
  }

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }
  if (to.meta.guest && isLoggedIn) {
    next('/dashboard')
    return
  }
  if (to.meta.roles && isLoggedIn) {
    if (!(to.meta.roles as Role[]).includes(userRole as Role)) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
