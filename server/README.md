# 服务端

## 初始化（由韩召然在第 1 周完成）

用 Spring Initializr 在本目录生成工程：

- Maven，Java 17，Spring Boot 3.x 最新稳定版
- Group `com.linlijiewu`，Artifact `server`，包名 `com.linlijiewu.server`
- 依赖：Spring Web、WebSocket、Validation、MyBatis Framework、MySQL Driver
- 另加：`io.jsonwebtoken:jjwt-*`（JWT）、`org.springframework.security:spring-security-crypto`（BCrypt）

## 包结构

```
com.linlijiewu.server
├── config        WebMvc（Token 拦截器）、WebSocket、CORS
├── common        统一响应、错误码、全局异常处理
├── auth          注册登录、JWT
├── item          物品、检索、距离计算
├── image         图片上传与重新编码
├── borrow        订单、状态机、超时定时任务、日志
└── chat          会话、消息、WebSocket 推送
```

## 本地运行

1. 本机 MySQL 执行 `../docs/数据库设计.sql`
2. 复制 `src/main/resources/application-example.yml` 为 `application-dev.yml` 并填写本地数据库密码
3. `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
