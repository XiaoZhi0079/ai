<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <h2>欢迎回来，{{ userStore.username }}</h2>
      <p class="dashboard-hero__text">
        AI 是统一入口，用于知识问答、网络检索与教务查询。
      </p>

      <div class="dashboard-hero__actions">
        <el-button type="primary" class="hero-btn" @click="router.push('/chat')">开始 AI 对话</el-button>
      </div>

      <div class="capability-tags">
        <span class="capability-tags__label">支持：</span>
        <span v-for="tag in capabilityTags" :key="tag" class="capability-tag">{{ tag }}</span>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <h3>常用功能</h3>
        </div>
      </div>

      <div class="action-grid">
        <button
          v-for="card in cards"
          :key="card.title"
          class="action-card"
          type="button"
          @click="router.push(card.path)"
        >
          <div class="action-card__icon" :style="{ '--icon-color': card.color }">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div class="action-card__body">
            <strong>{{ card.title }}</strong>
            <p>{{ card.desc }}</p>
          </div>
        </button>
      </div>
    </section>
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

const capabilityTags = ['知识问答', '网络检索', '教务查询']

const cards = computed(() => {
  const base = [
    { title: '知识库', desc: '查看与管理知识文档、知识问答资源', icon: 'UploadFilled', color: '#1b9ba8', path: '/rag' }
  ]
  if (userStore.isAdmin || userStore.isTeacher) {
    base.push(
      { title: '学生管理', desc: '查看与维护学生档案及相关信息', icon: 'Reading', color: '#2d8c63', path: '/students' },
      { title: '课程管理', desc: '管理课程信息与教学相关数据', icon: 'Notebook', color: '#d38a2a', path: '/courses' }
    )
  } else {
    base.push(
      { title: '课程信息', desc: '快速查看课程安排与课程相关信息', icon: 'Notebook', color: '#d38a2a', path: '/courses' },
      { title: '成绩信息', desc: '查看与访问个人或教学相关成绩数据', icon: 'TrendCharts', color: '#6f63d9', path: '/grades' }
    )
  }

  if (userStore.isAdmin) {
    base.push({ title: '用户管理', desc: '管理系统用户与角色权限配置', icon: 'User', color: '#ad4f84', path: '/users' })
  }

  return base
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-hero {
  padding: 20px 4px 8px;
}

.dashboard-hero h2 {
  margin: 0;
  color: #17324d;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.15;
}

.section-heading__eyebrow,
.dashboard-hero__text {
  max-width: 560px;
  margin: 14px 0 0;
  color: #5f7288;
  font-size: 16px;
  line-height: 1.7;
}

.dashboard-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.hero-btn {
  min-height: 50px;
  padding: 0 26px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 700;
}

.capability-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.capability-tags__label {
  color: #72849a;
  font-size: 13px;
  font-weight: 600;
}

.capability-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 13px;
  border: 1px solid rgba(201, 215, 229, 0.9);
  border-radius: 999px;
  background: #f8fbfe;
  color: #2c4c69;
  font-size: 13px;
  font-weight: 600;
}

.dashboard-section {
  padding: 18px 0 0;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-heading h3 {
  margin: 0;
  color: #17314d;
  font-size: 24px;
}

.section-heading p {
  margin: 0;
  color: #71859b;
  font-size: 14px;
  line-height: 1.7;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid rgba(203, 216, 229, 0.8);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.action-card:hover {
  transform: translateY(-1px);
  border-color: rgba(76, 146, 212, 0.36);
  background: #fbfdff;
  box-shadow: 0 10px 22px rgba(20, 44, 74, 0.06);
}

.action-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 54px;
  height: 54px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--icon-color) 14%, white);
  color: var(--icon-color);
}

.action-card__body {
  flex: 1;
  text-align: left;
}

.action-card__body strong {
  display: block;
  color: #18324d;
  font-size: 17px;
}

.action-card__body p {
  margin: 4px 0 0;
  color: #70839a;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1100px) {
  .action-grid {
    grid-template-columns: 1fr;
  }

  .section-heading {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .action-card {
    align-items: flex-start;
  }
}
</style>
