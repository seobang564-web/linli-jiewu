# Android 客户端

Java + XML 工程，包名固定为 `com.linlijiewu.app`，最低 API 24。
启动页为登录注册（`ui/auth/AuthActivity`），进入后是底部导航的首页：发现、订单、消息、我的，中间按钮发布。
全部页面按 `docs/ui-prototype.html` 实现。第 2 周的注册/登录、发布/编辑、图片上传、物品详情、我的发布已接真实服务端。
第三周发现/浏览、申请、订单与个人借还统计已接真实接口，页面不再回退到示例物品或固定订单。取物点选择已接高德逆地理与关键词地点搜索；物品详情、订单详情的导航按钮调起高德地图（未安装时打开高德网页版）。聊天仍待接入。第一周服务连接检查页从“我的 → 服务连接”进入。

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
debug 可用 `-PdebugMockLocation=纬度,经度` 临时指定联调坐标，release 始终不生效；未配置时走真实高德定位。分类浏览使用地图最近一次真实定位，无坐标时提示返回首页定位。

## 构建与测试

在仓库根目录的 PowerShell 7 中运行：

```powershell
.\scripts\Build-Week1.ps1 -Component Android
```

脚本运行 APK 编译、网络层单元测试及 Lint。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。
11 项网络/客户端规则测试使用本地 MockWebServer，不请求云服务器；覆盖路径、鉴权、注册、multipart、旧请求 401、图片 URL、真实附近查询编码、借用幂等请求、状态操作和身份按钮规则。
正式网络请求异步执行，不打印 Token，也不关闭 TLS 证书校验。
本地双账户验收：先运行 `scripts/Start-Week2-Smoke.ps1`，构建时用 `-PserverBaseUrl=http://10.0.2.2:18080/api/`。A 发布附近物品，B 从附近列表/分类搜索或按编号详情提交借用，A 在“我借出的”接受并确认交接，B 在“我借入的”申请归还，A 确认归还后物品重新可借。附近检索仅显示 AVAILABLE 的真实物品；详情和订单均重新向服务器核对状态。
申请在发送前按账户和物品保存 requestId 与请求快照，失败重试、页面重建或应用重启继续使用同一请求；重新填写需显式确认。退出登录不会让另一个账户读取这份申请。搜索会取消旧请求并校验递增序号，旧响应不能覆盖新条件。
草稿暂存 SharedPreferences 和应用私有图片文件，按账户隔离；SQLite 草稿仍由第 3 周负责人实现。Token 会话不接受旧的模拟登录标记。

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

- `ui/auth`、`ui/main`、`ui/discover`、`ui/browse`、`ui/item`、`ui/publish`、`ui/orders`、`ui/chat`、`ui/profile`：各页面。
- `ui/common`：`Ui` 通用方法、`Motion` 动效、`LoadingDialog` 吉祥物加载弹层、`Session` 登录态。
- `ui/bootstrap`：第一周服务连接检查页。
- `data/model`：物品、订单、会话、消息等模型；`data/sample`：示例数据，接口接入后移除。
- `data/remote`：Retrofit 接口、DTO、TokenProvider 与拦截器。
- `data/repository`：页面调用入口。
- `data/local`、`util` 按后续模块需要添加。

## 界面资源

- 颜色、字号、组件样式集中在 `res/values/colors.xml`、`styles.xml`、`dimens.xml`。
- 图标为线性矢量图（`ic_*`、`ic_nav_*`），吉祥物插画为 `mascot_*`。
- 转场与按压动画在 `res/anim`、`res/animator`、`res/interpolator`；系统关闭动画时自动跳过。
