# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目身份

这是 **校园失物招领平台** 的后端项目，与 `C:\Users\kangc\frontend` 配套使用。项目描述为"校园失物招领平台后端"。

## 技术栈（精确版本）

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.13 | 核心框架 |
| Java | 17 | 运行环境 |
| MyBatis Plus | 3.5.9 | ORM（spring-boot3-starter） |
| MySQL | 8.0 | 数据库（`lost_found` 库） |
| jjwt | 0.11.5 | JWT 签发/验证（HMAC-SHA256） |
| Lombok | 1.18.36 | 代码简化 |
| Knife4j | 4.4.0 | Swagger 文档（`/doc.html`） |
| fastjson2 | 2.0.43 | JSON 处理（已引入未使用） |
| hutool-all | 5.8.25 | 工具库（已引入未使用） |

- **构建工具**：Maven（`mvnw` wrapper）
- **项目坐标**：`com.lostfound:backend:0.0.1-SNAPSHOT`

## 常用命令

```bash
mvnw clean compile           # 编译
mvnw spring-boot:run         # 启动（开发模式，热重载）
mvnw clean package           # 打包 fat JAR
mvnw test                    # 运行测试
java -jar target/backend-*.jar  # 运行 fat JAR
```

- 启动后 API 文档地址：`http://localhost:8080/doc.html`
- `spring-boot-devtools` 已引入，IDE 中修改代码自动热重载

## 包架构

```
com.lostfound.backend
├── BackendApplication.java          # 主启动类
├── config/
│   ├── SecurityConfig.java          # Spring Security + CORS + BCrypt
│   └── MyBatisMetaObjectHandler.java # 自动填充 createTime/updateTime
├── controller/
│   ├── AuthController.java          # /api/auth/login, /api/auth/register
│   └── UserController.java          # /api/user/info
├── service/
│   ├── AuthService.java             # 接口
│   └── impl/AuthServiceImpl.java    # 登录/注册业务实现
├── mapper/
│   └── UserMapper.java              # MyBatis-Plus BaseMapper<User>
├── entity/
│   └── User.java                    # 用户实体（唯一实体）
├── dto/
│   ├── LoginDTO.java                # 登录请求（username + password）
│   └── RegisterDTO.java             # 注册请求（username/password/nickname/email）
├── vo/
│   ├── LoginVO.java                 # 登录响应（含 token + 用户信息）
│   └── UserInfoVO.java              # 用户信息响应
├── security/
│   └── JwtAuthenticationFilter.java # OncePerRequestFilter：JWT → SecurityContext
├── utils/
│   └── JwtUtil.java                 # JWT 生成/验证/解析工具
└── common/
    ├── result/Result.java           # 统一响应体 { code, message, data }
    └── exception/
        ├── BusinessException.java   # 自定义业务异常
        └── GlobalExceptionHandler.java # @RestControllerAdvice
```

## 关键架构细节

### 认证链路

```
请求 → JwtAuthenticationFilter（提取 Bearer Token）
     → JwtUtil.validateToken() → 解析 userId
     → UserMapper.selectById() → 获取 User
     → 校验 deleted != 1 && status != 1
     → UsernamePasswordAuthenticationToken → SecurityContextHolder
     → Controller 处理
```

### JWT 配置（application.yaml）

- **密钥**：`lost-found-platform-secret-key-2024-very-long-string`（硬编码在 yaml）
- **算法**：HMAC-SHA256（`SignatureAlgorithm.HS256`）
- **有效期**：7200 秒（2 小时）
- **负载**：`sub`=userId，`username`=username，`iat`=签发时间，`exp`=过期时间
- **refresh-token-expire**：yaml 配置了 7 天但从未使用（无 refresh 端点）

### SecurityConfig 白名单（无需认证）

```
/          /api/auth/**   /doc.html   /webjars/**
/v3/api-docs/**   /swagger-ui/**   /swagger-resources/**
OPTIONS 请求全部放行
```

### CORS 配置

- 允许来源：`http://localhost:*` 和 `http://127.0.0.1:*`
- 允许所有标准 HTTP 方法
- 允许携带凭证
- 预检缓存 3600 秒

### 统一响应格式

```json
{ "code": 200, "message": "操作成功", "data": { } }
```

- `Result.success(data)` → code 200
- `Result.error(code, msg)` → 自定 code

### 用户表（`user`，唯一已建表）

| 字段 | Java 类型 | DB 列 | 说明 |
|------|-----------|-------|------|
| id | Long | id | BIGINT 自增主键 |
| username | String | username | 唯一 |
| password | String | password | BCrypt 加密 |
| nickname | String | nickname | 显示名 |
| avatar | String | avatar | 头像 URL |
| phone | String | phone | 手机号 |
| email | String | email | 邮箱 |
| role | Integer | role | 0=普通用户，1=管理员 |
| status | Integer | status | 0=正常，1=禁用 |
| creditScore | Integer | credit_score | 信用分，默认 100 |
| successCount | Integer | success_count | 成功认领次数 |
| deleted | Integer | deleted | `@TableLogic`：0=正常，1=删除 |
| createTime | LocalDateTime | create_time | `FieldFill.INSERT` 自动填充 |
| updateTime | LocalDateTime | update_time | `FieldFill.INSERT_UPDATE` 自动填充 |

- 逻辑删除：MyBatis-Plus 自动追加 `deleted=0` 条件
- 自动填充：`MyBatisMetaObjectHandler` 处理时间戳
- 映射约定：`map-underscore-to-camel-case: true`

### 数据库配置

- **地址**：`jdbc:mysql://localhost:3306/lost_found`
- **编码**：UTF-8，时区 Asia/Shanghai
- **账号**：root（密码在 yaml 中明文存储）
- **DDL 策略**：无自动建表（需手动管理 schema）

### 当前 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/login` | 否 | 登录：验证密码 → 返回 JWT + 用户信息 |
| POST | `/api/auth/register` | 否 | 注册：校验唯一性 → BCrypt 加密 → 插入 |
| GET | `/api/user/info` | 是 | 返回当前登录用户信息 |

### 登录/注册逻辑细节（AuthServiceImpl）

- **登录**：查用户名 → 不存在返回 401 "用户名或密码错误"（防枚举）→ 检查 status（禁用返回 403）→ BCrypt 验密 → 生成 JWT
- **注册**：`@Transactional` → 校验 username 唯一 → 校验 email 唯一（如有）→ 构建 User（role=0, status=0, creditScore=100, successCount=0）→ 插入
- 默认昵称：不填则用 username

## 当前开发状态

### 已完成
- [x] Spring Boot 3.5 + Spring Security 骨架
- [x] JWT 无状态认证（签发 + 验证 + 拦截）
- [x] BCrypt 密码加密
- [x] 用户注册/登录 API
- [x] 统一响应体 `Result<T>`
- [x] 全局异常处理
- [x] MyBatis Plus 自动填充 + 逻辑删除
- [x] CORS 跨域支持
- [x] Knife4j 在线 API 文档
- [x] 用户表 `user`

### 待开发（按优先级）

- [ ] **帖子表** `post`：type/status/item_category/color/location/lost_time/user_id
- [ ] **数据字典表** `category`：物品大类/颜色/地点（三级树形）
- [ ] **帖子图片表** `post_image`
- [ ] **评论表** `comment`
- [ ] **消息通知表** `message`
- [ ] **匹配记录表** `match_record`
- [ ] **认领记录表** `claim_record`
- [ ] **操作日志表** `system_log`
- [ ] 帖子 CRUD API（发布/列表/详情/编辑/下架）
- [ ] 数据字典 API（级联查询）
- [ ] 匹配算法服务（梯度权重评分引擎）
- [ ] 匹配 API（手动触发 + 定时补偿任务）
- [ ] 文件上传 API（OSS 或本地存储）
- [ ] 信用分体系（增减规则 + 权限分层 AOP）
- [ ] 认领状态机（枚举 + 双确认机制）
- [ ] 管理端 API（用户管理/审核/统计/日志）
- [ ] RBAC 权限注解（`@PreAuthorize`）
- [ ] 单元测试 + 接口测试

## 开发规范

### 新增实体
- 在 `entity/` 下创建，使用 MyBatis-Plus 注解（`@TableName`、`@TableId`、`@TableLogic`）
- 需要时间戳自动填充的字段用 `@TableField(fill = ...)`
- 字段名用 camelCase，DB 列自动映射为 snake_case

### 新增 API
- Controller → Service（接口）→ ServiceImpl（实现）→ Mapper
- DTO（入参）放在 `dto/`，VO（出参）放在 `vo/`
- 入参校验用 `@Valid` + Jakarta Bean Validation
- 所有响应用 `Result<T>` 包装

### 业务异常
- 抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 统一处理
- 不要直接返回 error Result

### 安全
- 密码存储统一走 `BCryptPasswordEncoder`
- 接口级权限用 `@PreAuthorize("hasRole('ADMIN')")`（建议后续统一用）
- 禁止拼接 SQL，全部走 MyBatis-Plus 参数化查询

## 项目级开发文档

开发文档大纲位于桌面：`F:\Users\kangc\Desktop\校园失物招领平台_开发文档大纲.md`（含 .docx 副本）。数据库 schema 变更、新增 API、架构调整时需同步更新该文档。

## 安全规则（来自用户）

- 进行危险修改前先 git commit 保存当前状态
- 确保有回退路径再动手
- 不做破坏性操作（`--force`、`--no-verify` 等）
