# Android 客户端

第一周 Java + XML 工程，包名固定为 `com.linlijiewu.app`，最低 API 24。
当前入口为服务连接检查页，可真实调用 `/api/hello`；页面含初始、加载、成功、失败状态，销毁页面时取消请求。
发现、发布、借还、聊天及登录注册页面按开发计划由各负责人接入。

## 工具版本

- JDK 17；Android Studio 可直接打开本目录。
- Android Gradle Plugin 8.11.1，Gradle 8.13 Wrapper。
- compileSdk / targetSdk 35，Build Tools 35.0.0。
- Retrofit 2.11.0、OkHttp 4.12.0、Gson 转换器、Glide 4.16.0、Material Components 1.12.0。

## 本地配置

将 `local.properties.example` 复制为 `local.properties` 后，填写真实 SDK 目录及服务器地址。
Windows 路径中的盘符冒号需转义，例如 `sdk.dir=D\:/AndroidSdk`。
高德 Key 填入 `amap.key`，通过 Manifest placeholder 注入，不提交到 Git。

`server.baseUrl` 必须以 `/api/` 结尾：模拟器访问本机服务可用 `http://10.0.2.2:8080/api/`；
真机访问云端时填写实际公网地址。修改后重新构建安装。
命令行也可用 `-PserverBaseUrl=...` 临时覆盖，避免修改他人的本地配置。

## 构建与测试

在仓库根目录的 PowerShell 7 中运行：

```powershell
.\scripts\Build-Week1.ps1 -Component Android
```

脚本运行 APK 编译、网络层单元测试及 Lint。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。
网络测试使用本地 MockWebServer，不会请求云服务器；检查 `/api/` 路径保留、统一响应解析和 Bearer Token 头。
正式网络请求异步执行，不打印 Token，也不关闭 TLS 证书校验。

## 签名与高德交接

debug 构建已配置使用仓库中的 `keystore/debug.keystore`，不添加 applicationId 后缀。

```text
包名：com.linlijiewu.app
SHA1：97:05:24:BC:CF:1F:98:C9:D5:A8:26:12:55:57:9D:EB:1D:9C:D8:51
```

由方舒煜使用以上信息登记高德 Key，随后接入地图、定位 SDK 和隐私授权流程。
工程不自动发起定位，也不在未获得授权时初始化地图 SDK。
本期 HTTP/WS 部署已在 `network_security_config.xml` 放行明文流量，限制见开发约定。

## 代码位置

- `ui/bootstrap`：第一周连接检查入口，可由后续导航页面替换。
- `data/remote`：Retrofit 接口、DTO、TokenProvider 与拦截器。
- `data/repository`：页面调用入口。
- `data/local`、其他 `ui/` 页面和 `util` 按后续模块需要添加。
