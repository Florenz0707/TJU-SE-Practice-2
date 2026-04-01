# Address Service Documentation

## 微服务职能
**Service Name**: `address-service`
**端口**: 8086
**功能**: 处理外卖系统平台中用户收获/外卖配送地址（DeliveryAddress）相关的增删改查。

## 数据库与表结构
使用 MySQL 数据库，独立 schema (或者与其它服务共享数据库实例但表独立：`delivery_address`)。
此表存放用户的收货/配送地址基本信息。

### 核心实体/模型设计
```java
@Entity
@Table(name = "delivery_address")
public class DeliveryAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;       // 绑定的用户ID（购买者/收件人）

    @Column(name = "contact_name", nullable = false)
    private String contactName;    // 收件人姓名

    @Column(name = "contact_sex", nullable = false)
    private Integer contactSex;    // 收件人性别 1: 男, 0: 女

    @Column(name = "contact_tel", nullable = false)
    private String contactTel;     // 收件人电话号码

    @Column(nullable = false)
    private String address;        // 详细收货地址
}
```

## 外部暴露接口 (Public APIs)
供前端或客户端调用的一系列接口，主要通过 `gateway` 路由转发（前缀匹配 `/elm/api/addresses/**`）到本地 `/api/addresses` 处理。外部接口需要携带 `Authorization: Bearer <Token>`。由于 `address-service` 并非鉴权中心，已解耦引入 `JwtUtils` 在 Controller 侧做轻量级 Token JWT Claims 验证抽取 `userId`。

| 方法 | 路径 | 描述 | 请求参数 | 返回值 | 鉴权要求 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/addresses` | 添加收货地址 | RequestBody `DeliveryAddress` | `HttpResult<DeliveryAddress>` 带保存后的地址ID | 需登录（校验Token抽取用户ID做为customerId） |
| `GET` | `/api/addresses` | 查询我的收货地址 | Header 传 Token 即可 | `HttpResult<List<DeliveryAddress>>` | 同上 |
| `PUT` | `/api/addresses/{id}` | 更新收货地址 | PathVariable `id`, RequestBody 收货地址内容 | `HttpResult<DeliveryAddress>` | 同上，且校验目标地址所属人为当前用户 |
| `DELETE` | `/api/addresses/{id}` | 删除收货地址 | PathVariable `id` | `HttpResult<String>` | 同上，且校验目标地址所属人为当前用户 |

## 内部 RPC 接口 (Internal APIs)
提供给系统内其他微服务模块通过 HTTP / OpenFeign 或者直接内网调用的接口，通常以 `/internal/**` 为前缀。

| 方法 | 路径 | 描述 | 请求参数 | 返回值 | 补充说明 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/internal/addresses/user/{userId}` | 根据用户ID获取该用户的所有收货地址列表 | PathVariable `userId` | `ResponseEntity<List<DeliveryAddress>>` | 目前通常用于 Order 等相关模块跨库拉取地址快照使用。不再需要网关层面的 Token 限制。该服务由 `AddressInnerController` 根据 `AddressInternalService` 进行统一处理。 |

## 迁移细节说明
在此次由 `elm-v2.0` (Monolith) 分拆升级至 `elm-cloud` 微服务架构迁移中的核心要点：
1. **解除对 userService 的循环依赖**： 原单体项目中强依赖 `userService.getUserWithAuthorities()` 和 `ResponseCompatibilityEnricher` 来装填业务响应。现迁移过程中，取消了 `address-service` 中强引用 Security 的内容和跨模块的冗余查询（取消 Address 返回时的 customer 对象丰富操作），由前端自行解析或直接通过轻便的 `customerId` 做约束和引用。
2. **轻量鉴权（JwtUtils引入）**： 独立的微服务没有且没必要加载一整套完整的 JWT 过滤体系。因此复制并实现了一个用于提取 `Authorization` 里 Payload Claim `"id"`（即 userId）的简易反序列化解析组件 `JwtUtils`，保持地址业务与业务方身份的纯解耦！
3. **消除 Event-Sourcing (InternalOrderClient 代理) 的过度设计**：原代码存在通过 `internalOrderClient` 变相生成创建请求的问题。现在 Address 完全具备自己完整的存储模型和库调用操作。
