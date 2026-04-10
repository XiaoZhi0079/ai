# Repository Guidelines

## 项目结构与模块组织
本仓库是基于 Vite 的 Vue 3 + TypeScript 前端项目。核心代码位于 `src/`：`src/api` 存放接口请求封装，`src/components` 放通用组件，`src/layouts` 放页面骨架，`src/router` 管理路由，`src/stores` 管理 Pinia 状态，`src/types` 定义共享类型，`src/views/<feature>` 按业务页面拆分。入口文件为 `src/main.ts` 和 `src/App.vue`。构建产物输出到 `dist/`，不要直接修改。

## 构建、测试与开发命令
- `npm install`：安装项目依赖。
- `npm run dev`：启动本地开发环境。
- `npm run build`：先执行 `vue-tsc -b` 类型检查，再生成生产构建。
- `npm run preview`：本地预览 `dist/` 中的构建结果。

## 编码风格与命名约定
遵循现有 Vue 单文件组件写法，优先使用 `<script setup lang="ts">`。统一使用 2 个空格缩进、无分号 TypeScript 风格，并通过 `@/` 引用 `src` 下模块。组件和页面文件使用 PascalCase，如 `ChatMessage.vue`、`DashboardView.vue`；业务目录使用小写命名，如 `src/views/chat`。函数、变量使用 camelCase，Pinia 仓库保持 `useXStore` 命名。

## 测试规范
当前 `package.json` 中未配置独立测试框架，因此提交前至少执行一次 `npm run build` 作为回归检查。新增工具函数、状态逻辑或可独立验证的模块时，建议补充 `*.spec.ts` 测试文件，可放在相邻目录或 `src/**/__tests__/` 下，并同步把测试命令写入 `package.json`。

## 提交与合并请求规范
近期提交信息以简短中文描述为主，例如 `前端优化`、`登录页美化`。建议一次提交只聚焦一个改动点，标题简洁且可快速识别。提交 PR 时应包含：变更摘要、影响的页面或模块、手动验证步骤、关联 issue（如有），以及界面改动前后截图。

## 安全与配置提示
不要提交密钥、令牌或带环境信息的接口地址。请求层相关调整尽量集中在 `src/api/request.ts`，涉及登录态、本地缓存或路由守卫的修改时，要重点检查 `localStorage` 恢复逻辑和权限跳转是否正常。
