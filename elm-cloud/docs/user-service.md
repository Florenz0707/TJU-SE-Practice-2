# user-service 微服务详情

## 1. 功能概述
user-service 主要负责系统中用户的核心身份验证及基本信息管理。功能包括：
- 用户注册（含密码入库和加密）。
- 用户登录（验证密码并返回带权限角色的 JWT Token）。
- 获取及更新当前登录用户信息。
- 各类 IAM 身份凭证等管理。

## 2. 数据库与表结构
- 数据库连接：`elm` (`elm_user` / `elm_person` 等，通常取决于数据库设计)。
- 核心实体与表：
  - `User` 表 (`elm_user`)：包含账号(`username`)、加密后的密码(`password`)、状态(`enabled`)以及权限信息。
  - `UserDao` 和 `Authority` 等。

## 3. 提供的接口

### 3.1 外部暴露接口 (外部通过 Gateway 调用)
- **POST /elm/api/auth** (`/api/auth`)
  用于登入鉴权，传入凭证返回 JWT Token。
- **POST /elm/api/persons** (`/api/persons`)
  用户通过前台完成开放式注册功能。
- **GET /elm/api/user** (`/api/user`)
  使用当前 Bearer token 换取最新登陆用户信息及所拥有角色列表。

### 3.2 内部接口
该服务作为 IAM 骨干节点，通过 JWT Filter 保障所有的访问者凭证能被转换为其它服务（如积分、订单）的可解析 User 信息对象。

## 4. 迁移细节
1. 从单体 Elm boot 中将 `UserService`、`AuthenticationRestController` 的相关逻辑及安全拦截器 `JwtRequestFilter` 完整拆出并剥离到 `user-service`。
2. 重构了 `User` 实体类的 `password` 字段注解，使用 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`，在反序列化接受注册密码时可正确存写，在输出 JSON 时依然保持隐蔽（避免将密文传回前台）。
3. 网关配置中，配置 `- Path=/elm/api/authenticate, /elm/api/register, /elm/api/user, /elm/api/persons/**, /elm/api/auth/**` 等完整覆盖所需暴露的前端身份调用。
