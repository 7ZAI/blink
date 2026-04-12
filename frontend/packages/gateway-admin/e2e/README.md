# Gateway Admin E2E 测试

本目录包含 gateway-admin 前端项目的 Playwright E2E 测试。

## 测试覆盖范围

### 路由管理模块 (route-management.spec.ts)

测试除 CRUD 外的以下功能：

1. **路由推送** - 推送选中的路由到实例
2. **全量推送** - 一键推送分组内所有启用路由
3. **同步到实例** - 广播或指定实例同步
4. **批量状态更新** - 批量启用/禁用路由
5. **导出路由** - 导出选中路由为 JSON
6. **导入路由** - 从 JSON 导入路由配置
7. **克隆路由** - 复制现有路由
8. **历史记录** - 查看路由变更历史
9. **回滚路由** - 回滚到历史版本
10. **存储模式切换** - Redis/Nacos 模式切换
11. **推送状态显示** - 查看推送状态和时间
12. **搜索筛选** - 路由搜索和分页
13. **表单模式切换** - 表单/JSON 编辑模式
14. **权限控制** - 按钮权限验证

## 环境准备

### 1. 安装依赖

```bash
cd frontend/packages/gateway-admin
npm install
```

### 2. 安装 Playwright 浏览器

```bash
npx playwright install chromium
```

### 3. 配置环境变量

复制环境变量模板并修改：

```bash
cp e2e/.env.example e2e/.env.local
```

编辑 `.env.local` 文件，配置以下变量：

```env
BASE_URL=http://localhost:3001
API_BASE=http://localhost:8008
TEST_USER=admin
TEST_PASSWORD=123456
```

### 4. 启动服务

确保后端服务和前端开发服务器都在运行：

```bash
# 后端服务（另一个终端）
cd blink-gateway/gateway-admin
./gradlew bootRun

# 前端开发服务器
cd frontend/packages/gateway-admin
npm run dev
```

## 运行测试

### 运行所有测试

```bash
npm run test:e2e
```

### 运行特定测试文件

```bash
npx playwright test e2e/route-management.spec.ts
```

### 运行特定测试用例

```bash
npx playwright test -g "应能推送选中的路由"
```

### 交互式 UI 模式

```bash
npm run test:e2e:ui
```

### 调试模式

```bash
npm run test:e2e:debug
```

### 生成测试代码

```bash
npm run test:e2e:codegen
```

## 查看测试报告

测试完成后，查看 HTML 报告：

```bash
npm run test:e2e:report
```

## 目录结构

```
e2e/
├── .env.example           # 环境变量模板
├── helpers/
│   └── route-helpers.ts   # 测试辅助函数
├── route-management.spec.ts  # 路由管理测试用例
└── README.md              # 本文档
```

## 测试数据

测试过程中会创建以下测试路由：

- `test-playwright-route` - 主测试路由
- `test-playwright-route-clone` - 克隆功能测试路由
- `test-import-route-1` - 导入功能测试路由

测试结束后会自动清理这些数据。

## 常见问题

### 1. 登录超时

确保测试用户凭据正确，且后端服务正常运行。

### 2. 元素找不到

检查页面是否完全加载，可能需要增加 `waitForTimeout` 时间。

### 3. 测试失败重试

测试失败后会自动截图和录制视频，可在 `test-results/` 目录查看。

## 扩展测试

如需添加新的测试用例，请参考现有的测试结构：

```typescript
test.describe('新功能测试', () => {
  test.beforeEach(async () => {
    await navigateToRouteManagement(page)
  })

  test('应能执行新功能', async () => {
    // 测试代码
  })
})
```
