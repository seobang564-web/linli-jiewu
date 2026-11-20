# 邻里借物地图

基于 Android 的附近闲置物品借用系统（移动应用开发及安全 课程设计）。
App 名称「邻里借物」，标语：好东西出门串个门，记得回家。

小组成员：方舒煜、隋骐泽、韩召然

## 当前进度

| 模块 | 状态 |
| --- | --- |
| 工程骨架 | Android 与 Spring Boot 工程已建立，`/api/hello`、数据库健康检查已上云验收 |
| 地图定位 | 高德 3D 地图、定位、隐私合规弹窗已接入；debug 可配置固定定位 |
| 客户端界面 | 已按 `docs/ui-prototype.html` 实现全部页面与动效，数据暂为本地示例 |
| 业务接口 | 登录注册、物品、借用、聊天接口待服务端实现后替换示例数据 |

### 客户端页面

| 页面 | 说明 |
| --- | --- |
| 登录 / 注册 | 启动页；账号密码校验，登录态暂存本地（模拟） |
| 发现 | 高德地图 + 附近物品标记，500 m / 1 km / 3 km / 5 km 半径切换，底部抽屉列出附近物品与分类 |
| 浏览 | 按分类浏览，左侧分类栏，空状态插画 |
| 物品详情 / 申请借用 | 图片视差、物主信息；选择取用日期与时段后提交申请 |
| 发布 | 从底部弹出；标题、分类、描述、取物点，支持保存草稿 |
| 取物点选择 | 拖动地图选点，返回地址与坐标 |
| 订单 / 订单详情 | 我借的、借出的两组订单，状态筛选，进度时间轴 |
| 消息 / 聊天 | 会话列表、聊天气泡、正在输入提示 |
| 我的 | 个人信息、借用统计、功能入口、退出登录；「服务连接」可进入第一周连接检查页 |

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
- 服务端：`server/target/app.jar`；运行 `.\scripts\Package-Server.ps1` 生成部署包，按 `docs/部署说明.md` 上传部署。
