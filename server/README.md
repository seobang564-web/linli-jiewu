# 服务端

第一周工程：Java 17、Spring Boot 3.5.16、Maven 3.9.11 Wrapper、MyBatis 3.0.5、MySQL 8。
Web、WebSocket、Validation、BCrypt 与 jjwt 依赖已固定版本；账户、借还和聊天业务按后续周计划实现。

## 构建与测试

在仓库根目录的 PowerShell 7 中运行：

```powershell
.\scripts\Build-Week1.ps1 -Component Server
```

脚本优先寻找已安装的 JDK 17，不修改系统 Java 设置。也可显式传入 `-JavaHome`。
不需要全局安装 Maven；首次运行 Wrapper 会下载 Maven，缓存放在根目录 `.tools/`。
产物固定为 `server/target/app.jar`。

测试使用内存 H2 验证 HTTP、响应结构、MyBatis 基础表检查、缺表失败和错误脱敏；不会连接或修改云端数据库。
MySQL 8 的实际连接及六张真实表须通过云端 `/api/health` 验收。

## 本地运行

先在本机 MySQL 初始化一次 `../docs/数据库设计.sql`，创建专用数据库用户。
在当前 PowerShell 进程中设置 `DB_USERNAME`、`DB_PASSWORD`，然后在本目录运行：

```powershell
.\mvnw.cmd spring-boot:run
```

需要 JDK 17；如本机默认 Java 不是 17，应在当前终端配置 `JAVA_HOME`。
默认连接 `localhost:3306/linli_jiewu`，支持通过 `DB_URL` 覆盖。每个 MySQL 连接初始化会话时区为 `+08:00`。
`spring.sql.init.mode=never`，启动与部署均不会重新建表或清空数据。

## 验收

- `GET /api/hello`：进程与路由检查，不依赖数据库。
- `GET /api/health`：MyBatis 连接与六张基础表检查，失败返回 HTTP 503。
- 未实现业务接口不提供模拟成功响应。

## 配置

`src/main/resources/application-example.yml` 是可提交的无密钥模板。
生产配置放 `/opt/linli/application-prod.yml`；实际密码与 JWT 密钥通过 `/etc/linli/server.env` 注入。
JWT 配置为后续登录功能预留，第一周没有实现业务鉴权。

| 环境变量 | 默认值 / 用途 |
| --- | --- |
| `PORT` | `8080` |
| `DB_URL` | 本机 MySQL 8，数据库 `linli_jiewu`，上海时区，连接与读超时 |
| `DB_USERNAME` | `linli` |
| `DB_PASSWORD` | 数据库密码；生产环境必须提供 |
| `UPLOAD_DIR` | 本地 `./uploads`；部署后 `/opt/linli/uploads` |
| `JWT_SECRET` | 部署脚本生成，为第 2 周登录功能预留 |

打包部署见 [部署说明](../docs/部署说明.md)。
