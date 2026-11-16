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
