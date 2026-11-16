# Android 客户端

## 初始化（第 1 周完成）

用 Android Studio 在本目录新建工程：

- 模板：Empty Views Activity
- Language：**Java**，Build configuration language 用默认
- Package name：`com.linlijiewu.app`（不可再改，高德 Key 绑定此包名）
- Minimum SDK：API 24

## 依赖

Retrofit + Gson 转换器、OkHttp（含 WebSocket）、Glide、Material Components、高德 3D 地图 SDK 与定位 SDK（版本以高德开放平台最新文档为准）。

## 本地配置 `local.properties`（不提交）

```
amap.key=<高德 Android Key>
server.baseUrl=http://<服务器IP>:8080/api/
```

在 `app/build.gradle` 中读取，`amap.key` 通过 `manifestPlaceholders` 注入 Manifest，`server.baseUrl` 写入 `BuildConfig`。

## 签名

`keystore/debug.keystore` 为全组统一的 debug 签名（第 1 周生成后提交）。在 `app/build.gradle` 的 `signingConfigs.debug` 指向它，SHA1 用以下命令查看后登记到高德控制台：

```
keytool -list -v -keystore keystore/debug.keystore -alias androiddebugkey -storepass android
```

## 包结构

```
com.linlijiewu.app
├── ui/discover   发现页（地图、搜索、列表）
├── ui/publish    发布与编辑
├── ui/item       物品详情
├── ui/borrow     借入借出、订单详情
├── ui/chat       会话列表、聊天页
├── ui/me         个人中心、登录注册
├── data/remote   Retrofit 接口、DTO、WebSocket 客户端
├── data/local    SQLite 草稿与搜索历史
├── data/repository
└── util
```
