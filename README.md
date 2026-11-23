# 邻里借物地图

基于 Android 的附近闲置物品借用系统（移动应用开发及安全 课程设计）。
App 名称「邻里借物」，标语：好东西出门串个门，记得回家。

小组成员：方舒煜、隋骐泽、韩召然

## 当前进度

| 模块 | 状态 |
| --- | --- |
| 工程骨架 | Android 与 Spring Boot 工程已建立，`/api/hello`、数据库健康检查已上云验收 |
| 地图定位 | 高德 3D 地图、定位、隐私合规弹窗已接入；debug 可配置固定定位 |
| 客户端界面 | 登录、发布、详情、附近列表、分类搜索、申请借用、订单与个人统计已接真实接口；聊天待接入 |
| 业务接口 | 已实现账户、JWT、图片上传、物品管理、附近检索及借还事务/状态机；聊天与自动超时按第 4 周推进 |

### 客户端页面

| 页面 | 说明 |
| --- | --- |
| 登录 / 注册 | 真实服务端验证，保存有期限的 Token，401 清除旧登录态 |
| 发现 | 高德地图 + 附近物品标记，500 m / 1 km / 3 km / 5 km 半径切换，底部抽屉列出附近物品与分类；标记与列表联动（先选中定位，再点进入详情），可切换到任意位置附近检索 |
| 浏览 | 真实分类与关键词检索，300ms 防抖、半径切换、分页、加载/失败重试/空状态 |
| 物品详情 / 申请借用 | 真实详情与图片；物主编辑/下架；借用者提交真实申请，失败重试保留同一 requestId |
| 发布 | 选图压缩 → 上传 → 发布，提交真实交接点坐标；草稿按账户隔离 |
| 取物点选择 | 拖动地图选点，下方列出高德周边地点，支持关键词搜索地点，返回名称与 GCJ-02 坐标 |
| 订单 / 订单详情 | 真实借入/借出分页、状态筛选、快照、时间轴和按身份显示的七种状态操作 |
| 消息 / 聊天 | 会话列表、聊天气泡、正在输入提示 |
| 我的 | 真实登录信息、发布与借还数量、分页列表、按编号查看物品、退出登录；保留服务连接入口 |

动效：页面推入 / 弹出转场、发布页上滑、按压缩放、列表错峰进场、数字滚动；
网络加载时显示吉祥物加载弹层（背景变暗）。系统关闭动画时全部自动跳过。

## 仓库结构

```
linli-jiewu/
├── android/                Android 客户端（Java + XML，Android Studio 工程）
│   └── app/src/main/java/com/linlijiewu/app/
│       ├── ui/             各页面：auth、main、discover、browse、item、publish、orders、chat、profile、bootstrap
│       ├── ui/common/      通用工具：Ui、Motion 动效、LoadingDialog、Session
│       ├── data/model/     物品、订单、会话等数据模型
│       ├── data/sample/    示例数据（接口接入后移除）
│       ├── data/remote/    Retrofit 接口、DTO、拦截器
│       └── data/repository/
├── server/                 服务端（Java 17 + Spring Boot 3 + MyBatis + MySQL 8）
├── scripts/                构建、打包、验收脚本（PowerShell 7）
└── docs/
    ├── 开发约定.md          分支、提交、代码规范、密钥管理（开工前必读）
    ├── 接口文档.md          前后端唯一接口契约，改接口先改这里
    ├── 数据库设计.sql       MySQL 建表脚本
    ├── 开发计划.md          六周任务与分工
    ├── 部署说明.md          云服务器部署步骤
    ├── ui-prototype.html   可交互界面原型（浏览器直接打开）
    ├── screenshots/        联调截图
    └── 周记录/              每周进度
```

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 客户端 | Android Studio、Java 17、XML 布局、Material Components、Retrofit + OkHttp、Gson、Glide |
| 地图定位 | 高德 3D 地图 SDK、定位 SDK、搜索 SDK（坐标统一 GCJ-02） |
| 服务端 | Spring Boot 3、Spring Web、Spring WebSocket、MyBatis、jjwt、BCrypt |
| 数据 | MySQL 8（共享数据）、SQLite（客户端草稿与搜索历史）、服务端图片目录 |
| 协作 | Git + GitHub，功能分支 + Pull Request 审查 |

## 快速开始

1. 阅读 `docs/开发约定.md`。
2. 服务端：见 `server/README.md`。
3. 客户端：见 `android/README.md`。复制 `android/local.properties.example` 为 `local.properties`，
   填写 SDK 路径、服务器地址和高德 Key（不进 Git）。
4. 想先看界面效果，可在浏览器打开 `docs/ui-prototype.html`。

## 构建

在仓库根目录的 PowerShell 7 中运行：

```powershell
.\scripts\Build-Week1.ps1
```

需要 JDK 17、Android SDK Platform 35 / Build Tools 35.0.0；Gradle 和 Maven 使用仓库 Wrapper。
仅构建服务端加 `-Component Server`，仅 Android 加 `-Component Android`。
macOS / Linux 也可直接在 `android/` 下运行 `./gradlew assembleDebug`。

- APK：`android/app/build/outputs/apk/debug/app-debug.apk`
- 服务端：`server/target/app.jar`；运行 `.\scripts\Package-Server.ps1` 生成 `.artifacts/linli-week3-deploy.zip`。第 3 周沿用已有借还表；只有尚未完成第 2 周升级的环境才需先执行 `002_images.sql`。

本地联调可运行 `scripts/Start-Week2-Smoke.ps1`，使用 H2 测试库和 18080 端口。第三周不新增生产表结构；已有第二周环境保留数据库和图片更新服务即可。

第三周已接通“真实附近检索 → 详情/申请 → 物主接受/交接 → 申请归还 → 物主验收”的客户端及服务端流程。运行 `scripts/Verify-Week3.ps1` 验证 36 项真实 HTTP 检查；结果写入本机忽略目录 `.artifacts/week3-http-results.json`。独立模拟器双账户流程与截图见 `docs/周记录/第3周.md`。云端部署与手机安装新版 APK 均完成后，才能使用这批新接口；自动超时任务与聊天推送留待后续。
