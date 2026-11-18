# 邻里借物地图

基于 Android 的附近闲置物品借用系统（移动应用开发及安全 课程设计）。

小组成员：方舒煜、隋骐泽、韩召然

## 仓库结构

```
linli-jiewu/
├── android/            Android 客户端（Java + XML，Android Studio 工程）
├── server/             服务端（Java 17 + Spring Boot 3 + MyBatis + MySQL 8）
└── docs/
    ├── 开发约定.md      分支、提交、代码规范、密钥管理（开工前必读）
    ├── 接口文档.md      前后端唯一接口契约，改接口先改这里
    ├── 数据库设计.sql   MySQL 建表脚本
    ├── 开发计划.md      六周任务与分工
    └── 部署说明.md      云服务器部署步骤
```

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 客户端 | Android Studio、Java 17、XML 布局、Retrofit + OkHttp、Gson、Glide |
| 地图定位 | 高德 3D 地图 SDK、定位 SDK（坐标统一 GCJ-02） |
| 服务端 | Spring Boot 3、Spring Web、Spring WebSocket、MyBatis、jjwt、BCrypt |
| 数据 | MySQL 8（共享数据）、SQLite（客户端草稿与搜索历史）、服务端图片目录 |
| 协作 | Git + GitHub，功能分支 + Pull Request 审查 |

## 快速开始

1. 阅读 `docs/开发约定.md`。
2. 服务端：见 `server/README.md`。
3. 客户端：见 `android/README.md`。

## 第一周工程

Android 与 Spring Boot 工程骨架已建立。Hello 和数据库检查的接口契约见 `docs/接口文档.md`。
地图、页面导航及后续业务由各负责人按开发计划继续接入。

在仓库根目录的 PowerShell 7 中构建并运行本地检查：

```powershell
.\scripts\Build-Week1.ps1
```

需要已安装 JDK 17、Android SDK Platform 35 / Build Tools 35.0.0；Gradle 和 Maven 使用仓库 Wrapper。
仅构建服务端可加 `-Component Server`，仅 Android 可加 `-Component Android`。
客户端本地配置不进 Git，参考 `android/local.properties.example`。

服务端产物为 `server/target/app.jar`，APK 为 `android/app/build/outputs/apk/debug/app-debug.apk`。
运行 `.\scripts\Package-Server.ps1` 生成 `.artifacts/linli-week1-deploy.zip`，按 `docs/部署说明.md` 上传部署。
云端验收以实际执行和公网检查结果为准，进度见 `docs/周记录/第1周.md`。
