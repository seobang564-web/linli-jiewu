# 服务端

第一周工程：Java 17、Spring Boot 3.5.16、Maven 3.9.11 Wrapper、MyBatis 3.0.5、MySQL 8。
第 2 周增加注册登录、JWT、个人资料、图片上传和物品管理；第 3 周实现附近检索、借还申请事务、幂等、状态机和订单查询。自动超时与聊天业务留待第 4 周。WebP 使用 [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) 3.15.0 解码后重新编码为 PNG。

## 构建与测试

在仓库根目录的 PowerShell 7 中运行：

```powershell
.\scripts\Build-Week1.ps1 -Component Server
```

脚本优先寻找已安装的 JDK 17，不修改系统 Java 设置。也可显式传入 `-JavaHome`。
不需要全局安装 Maven；首次运行 Wrapper 会下载 Maven，缓存放在根目录 `.tools/`。
产物固定为 `server/target/app.jar`。

测试使用内存 H2 验证账户密码、鉴权、双账户物品访问、图片归属、JPG/PNG/WebP 解码、回滚文件清理、物品状态约束及健康检查；不会连接云端数据库。
借还测试覆盖完整借还/拒绝归还流程、34 组非法状态操作、所有操作的身份校验、联系方式隐藏、快照不随物品编辑改变、幂等重试、并发抢借/接受/取消/下架/再次借用和日志失败时事务回滚。测试配置将 H2 首次连接等待设为 15 秒，生产连接超时保持原配置。
附近检索增加 6 项集成测试，覆盖 499/500/501 米、按原始距离过滤再取整、跨日期变更线和极区、字面子串关键词、状态/分类、排序分页与预约后移出/取消后恢复。Maven verify 共 38 项测试通过。
MySQL 8 的真实迁移与七张表仍须在云端验收。已有环境先执行 `docs/migrations/002_images.sql`。

第 3 周没有生产表结构增量；`borrows`、`borrow_logs`、`items.active_borrow_id` 与幂等唯一键已在原始 `docs/数据库设计.sql` 中定义。部署前检查实际表字段、`uk_borrows_request(borrower_id, request_id)` 与 InnoDB 引擎；不要重复执行完整建表脚本。

## 本地运行

先在本机 MySQL 初始化一次 `../docs/数据库设计.sql`，创建专用数据库用户。
在当前 PowerShell 进程中设置 `DB_USERNAME`、`DB_PASSWORD`，然后在本目录运行：

```powershell
.\mvnw.cmd spring-boot:run
```

需要 JDK 17；如本机默认 Java 不是 17，应在当前终端配置 `JAVA_HOME`。
默认连接 `localhost:3306/linli_jiewu`，支持通过 `DB_URL` 覆盖。每个 MySQL 连接初始化会话时区为 `+08:00`。运行前需设置 `JWT_SECRET` 为至少 32 字节随机值的 Base64 编码；现有部署脚本生成的 48 字节密钥可直接沿用。
`spring.sql.init.mode=never`，启动与部署均不会重新建表或清空数据。

## 验收

- `GET /api/hello`：进程与路由检查，不依赖数据库。
- `GET /api/health`：MyBatis 连接与七张基础表检查，失败返回 HTTP 503。
- 未实现业务接口不提供模拟成功响应。

## 配置

`src/main/resources/application-example.yml` 是可提交的无密钥模板。
生产配置放 `/opt/linli/application-prod.yml`；实际密码与 JWT 密钥通过 `/etc/linli/server.env` 注入。
鉴权从请求 Token 解析身份；公开详情只返回物主 id/昵称，联系方式不公开。图片公开读取，上传和引用归属校验需要登录。

| 环境变量 | 默认值 / 用途 |
| --- | --- |
| `PORT` | `8080` |
| `DB_URL` | 本机 MySQL 8，数据库 `linli_jiewu`，上海时区，连接与读超时 |
| `DB_USERNAME` | `linli` |
| `DB_PASSWORD` | 数据库密码；生产环境必须提供 |
| `UPLOAD_DIR` | 本地 `./uploads`；部署后 `/opt/linli/uploads` |
| `JWT_SECRET` | 部署脚本生成，必填，不允许空密钥启动 |

无本地 MySQL 时，可在构建后运行 `scripts/Start-Week2-Smoke.ps1` 启动 H2 联调服务（默认 18080）。它使用测试配置和测试密钥，不连接生产库，重启后数据清空，仅供本地联调。

另开 PowerShell 7 终端运行 `scripts/Verify-Week3.ps1`，自动创建测试账户/物品，验证申请幂等、借还、拒绝验收、权限、日志和归还后释放，最后下架本次物品。结果不含密码或 Token。脚本默认只允许回环地址；人工执行远端验收需明确指定 `-BaseUrl` 和 `-AllowRemote`，会在该服务创建测试数据。

借还入口为 `/api/borrows`，全部要求 Token；支持 POST 申请、GET 分页、GET `/{id}`、GET `/{id}/logs`，以及 POST `/{id}/accept|reject|cancel|handover|return|confirm-return|deny-return`。状态请求体可省略；有请求体时使用 JSON。接受申请在锁内复核 12 小时到期时间；自动取消扫描与 WebSocket 推送尚未实现。
`GET /api/items` 提供真实附近检索：参数绑定做经纬度包围框粗筛，Java Haversine 精确过滤，原始距离与半径比较，返回整数 distanceM 后按 distanceM/id 排序。关键词为标题/描述字面子串，`%` 和 `_` 不作为通配符；默认仅 AVAILABLE，不允许查询 OFFLINE。

打包部署见 [部署说明](../docs/部署说明.md)。
