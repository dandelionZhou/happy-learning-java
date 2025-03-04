### 1. 技术栈选择

- **核心框架**: Spring Boot 3.2.x + Spring Cloud 2023.0.x
- **服务发现**: Spring Cloud Netflix Eureka 或 Consul
- **API 网关**: Spring Cloud Gateway
- **安全框架**: Spring Security 6 + OAuth2.1 + JWT
- **数据库**: PostgreSQL 15 + Redis 7 (缓存/会话存储)
- **构建工具**: Maven/Gradle
- **监控体系**: Micrometer + Prometheus + Grafana
- **日志系统**: ELK Stack (Elasticsearch + Logstash + Kibana)
- **容器化**: Docker + Kubernetes (生产部署)
- **API 文档**: Spring Doc OpenAPI 3



### 2.服务拆分方案

#### 2.1 API Gateway Service

API 网关处理路由、限流、熔断等



#### 2.2 Auth Service (核心鉴权服务)

**功能**：

- 用户注册/登录
- OAuth2.1 授权
- JWT 令牌颁发
- 权限管理(RBAC)
- 单点登录(SSO)
- 安全审计
- 多端登录支持（允许/限制多个设备同时登录）
- **刷新令牌机制**（确保长时间会话）
- 令牌黑名单（实现 Token 失效机制）
- **OAuth2 客户端管理**（不同应用的授权管理）
- **日志审计**（记录用户登录、授权、Token 使用情况）
- **2FA（双因素认证）**（提高安全性）
- **API 限流**（防止 Token 滥用）
- 安全性增强

- RSA 加密 JWT
- XSS、CSRF 保护
- IP 白名单/黑名单



auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/auth/
│   │   │   ├── config/               # 配置类
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   └── RedisConfig.java
│   │   │   │
│   │   │   ├── controller/           # API 端点
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   │
│   │   │   ├── dto/                  # 数据传输对象
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   └── RegisterRequest.java
│   │   │   │   └── response/
│   │   │   │       └── JwtResponse.java
│   │   │   │
│   │   │   ├── model/                # 数据实体
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   └── UserRole.java
│   │   │   │
│   │   │   ├── repository/           # 数据访问层
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RoleRepository.java
│   │   │   │
│   │   │   ├── security/             # 安全相关组件
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── JwtUtils.java
│   │   │   │   ├── UserDetailsImpl.java
│   │   │   │   └── OAuth2/
│   │   │   │       └── CustomOAuth2UserService.java
│   │   │   │
│   │   │   ├── service/              # 业务逻辑层
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── impl/
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       └── UserServiceImpl.java
│   │   │   │
│   │   │   └── exception/            # 异常处理
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── AuthException.java
│   │   │       └── ErrorCode.java
│   │   │
│   │   └── resources/
│   │       ├── db/migration/         # Flyway 脚本
│   │       │   └── V1__init.sql
│   │       ├── application.yml       # 主配置文件
│   │       └── application-dev.yml   # 开发环境配置
│   │
│   └── test/                         # 测试代码
│       └── java/com/example/auth/
│           ├── AuthServiceTest.java
│           └── SecurityConfigTest.java



#### 2.3 User Service

**功能**：

- 用户信息管理
- 个人资料维护
- 账户状态管理
- 审计日志记录