# elm-cloud 微服务改造最终汇报

---

## 1. 项目概述

### 1.1 改造目标
- **原项目**：elm-v2.0 单体应用（所有功能在一个服务里）
- **改造目标**：拆分为 elm-cloud 微服务架构
- **核心目的**：实现高可用、可扩展的外卖平台，一个服务挂掉不影响整体运行

### 1.2 技术选型说明
| 组件 | 技术方案 | 说明 |
|------|---------|------|
| 服务注册发现 | Eureka Server | 替代停更的 Netflix Eureka |
| 统一入口与路由 | Spring Cloud Gateway | 外部请求统一通过 Gateway（8080） |
| 负载均衡 | Spring Cloud LoadBalancer | 替代停更的 Netflix Ribbon |
| 配置中心 | Spring Cloud Config Server | 所有配置集中管理 |
| 动态刷新 | Spring Cloud Bus + RabbitMQ | 配置变更广播刷新 |
| 熔断降级 | Resilience4j | 替代停更的 Netflix Hystrix |
| 容器化 | Docker + Docker Compose | 一键部署，环境一致 |

---

## 2. 微服务拆分

### 2.1 基础设施服务
| 服务名 | 端口 | 职责 |
|------|------|------|
| eureka-server | 8761 | 服务注册与发现中心，所有服务自动注册 |
| config-server-1 | 8888 | 集中配置管理实例 1，各服务启动时拉取配置 |
| config-server-2 | 8889 | 集中配置管理实例 2，各服务启动时拉取配置 |
| gateway | 8080 | 统一入口，外部请求统一路由 |
| MySQL | 3306 | 数据库 |
| RabbitMQ | 5672 | 消息队列，用于配置刷新广播 |

### 2.2 业务微服务
| 服务名 | 端口 | Context Path | 实例数 | 数据库 | 主要功能 |
|------|------|-------------|--------|--------|----------|
| user-service | 8082 | /elm | 2 | elm_user | 用户登录、认证、信息管理 |
| merchant-service | 8085 | /elm | 2 | elm_merchant | 商家管理、店铺管理、商家应用审核 |
| product-service | 8083 | /elm | 2 | elm_catalog | 菜品管理、菜品分类、库存 |
| cart-service | 8086 | /elm | 2 | elm_order | 购物车管理、购物车增删改查 |
| order-service | 8084 | /elm | 2 | elm_order | 订单管理、状态流转、订单评价 |
| address-service | 8087 | /elm | 1 | elm_address | 收货地址管理 |
| points-service | 8081 | /elm | 1 | elm_points | 积分管理、积分记录、积分冻结和扣减 |
| wallet-service | 8088 | /elm | 1 | elm_wallet | 钱包管理、优惠券管理、钱包扣款和退款 |

---

## 3. 核心功能实现

### 3.1 服务注册与发现
- 所有服务启动后，自动向 Eureka Server 注册
- 服务间调用时，使用服务名而不是 IP/端口
- Eureka Server 会自动感知服务实例的上线和下线
- 查看 Eureka 控制台：http://localhost:8761

### 3.2 Gateway 统一路由
- 外部请求统一通过 Gateway（8080）访问
- Gateway 根据路径转发到对应服务
- Gateway 路由规则：
  - `/elm/api/orders/**` → order-service
  - `/elm/api/foods/**` → product-service
  - `/elm/api/cart/**` → cart-service
  - `/elm/api/merchants/**` → merchant-service
  - `/elm/api/users/**` → user-service
  - `/elm/api/wallet/**` → wallet-service
  - `/elm/api/points/**` → points-service
  - `/elm/api/addresses/**` → address-service

### 3.3 负载均衡
- 使用 **Spring Cloud LoadBalancer**
- 通过 `@LoadBalanced` 注解配置 RestTemplate
- 使用服务名进行调用（例如：`http://user-service/...`）
- 核心服务部署 **2个实例**，自动实现负载均衡
- 增加实例后无需改动代码，自动纳入负载

### 3.4 配置中心与动态刷新（Config Server + Bus）
- 配置中心采用 **双 Config Server 实例** 部署：`config-server-1` 和 `config-server-2`
- 所有中心化配置统一放在 `elm-cloud/config/` 目录下
- 各服务通过 `bootstrap.yml` 以 **discovery-first** 方式发现 `config-server`
- **配置降级策略**：Config Server 不可用时，自动降级使用本地配置
- RabbitMQ 与 Spring Cloud Bus 已接入，支持通过 `POST /actuator/busrefresh` 广播刷新事件
- 所有微服务 Controller 已加上 `@RefreshScope`
- `order-service` 提供 `/elm/api/orders/runtime-config` 接口，用于现场证明配置刷新结果

### 3.5 熔断降级（Resilience4j）
- 在微服务内部调用时使用 Resilience4j 实现熔断
- 熔断器有三种状态：
  1. **CLOSED（关闭）**：正常状态，请求正常通过
  2. **OPEN（打开）**：失败率超阈值，直接拒绝请求，等待10秒进入半开
  3. **HALF-OPEN（半开）**：允许少量请求通过，测试服务是否恢复
- **已实现熔断降级的服务**：
  - **product-service**：调用 merchant-service 获取店铺信息，失败返回空
  - **merchant-service**：调用 user-service 获取用户信息，失败返回默认用户
  - **cart-service**：调用 product-service 和 merchant-service 丰富数据，失败只显示购物车基本信息
  - **order-service**：调用 wallet、points、merchant、address、product、cart 等多个服务，每个调用都有独立的熔断器和降级方法

### 3.6 路径统一
- **Cart Service 路径统一改造**：
  - cart-service 的 context-path 从空改为 `/elm`，与其他服务保持一致
  - order-service 的内部调用路径已更新为 `http://cart-service/elm/internal/...`
  - Gateway 路由配置已更新，支持 `/elm` 前缀

---

## 4. 典型业务链路

### 4.1 用户下单流程
1. **用户登录**：前端 → Gateway → user-service → 验证并返回 JWT
2. **浏览店铺菜品**：前端 → Gateway → merchant-service + product-service
3. **加入购物车**：前端 → Gateway → cart-service
4. **创建订单**：前端 → Gateway → order-service
   - order-service 内部调用 address-service 获取地址
   - order-service 内部调用 wallet-service 检查优惠券
   - order-service 内部调用 points-service 冻结积分
   - order-service 内部调用 wallet-service 扣减钱包
   - order-service 内部调用 points-service 扣减积分
   - order-service 内部调用 cart-service 清空购物车
5. **支付完成**：订单状态变更为已支付

### 4.2 商家端流程
1. **商家登录**：前端 → Gateway → user-service
2. **查看我的订单**：前端 → Gateway → order-service
3. **管理菜品**：前端 → Gateway → product-service
4. **查看积分**：前端 → Gateway → points-service

---

## 5. 高可用与容错设计

### 5.1 多实例部署
- **核心服务**（user、merchant、product、cart、order）各部署 **2个实例**
- **其他服务**（address、wallet、points）部署 **1个实例**，因为调用频率较低

### 5.2 熔断降级设计
- **分层策略**：
  - Gateway 层：负责路由和负载均衡（暂未配置 Gateway 层熔断）
  - 微服务内部：使用 Resilience4j 熔断降级
- **典型容错场景**：
  - product-service 挂掉：用户浏览店铺菜品时，虽然菜品信息无法加载，但店铺详情依然可以显示
  - merchant-service 挂掉：product-service 调用失败，但菜品基本信息还能返回，只是店铺信息为空
  - wallet-service 挂掉：order-service 调用失败，订单创建流程会停止，但前端会有友好提示
  - Config Server 挂掉：各服务启动时自动用本地配置，不影响功能

---

## 6. 部署与验证

### 6.1 一键启动
```bash
cd elm-cloud
docker compose up -d
```

### 6.2 验证步骤
1. **查看 Eureka 控制台**：http://localhost:8761
   - 确认所有服务已注册
   - 确认核心服务有 2个实例
2. **访问前端**：http://localhost
   - 用户登录、浏览菜品、加入购物车、下单
   - 商家登录、查看订单、管理菜品、查看积分
3. **测试熔断**（可选）：
   - 停止某个服务：`docker compose stop product-service`
   - 访问相关页面，验证是否有降级处理
4. **测试配置刷新**（可选）：
   - 修改 `elm-cloud/config/order-service.yml`
   - 调用 `curl -X POST http://localhost:8888/actuator/busrefresh`
   - 访问 `http://localhost:8080/elm/api/orders/runtime-config`

---

## 7. 完成情况总结

### 7.1 已完成的功能
✅ **微服务拆分与部署**
- 从单体应用拆分为 9个微服务 + 4个基础设施服务
- 核心服务双实例部署，支持负载均衡

✅ **基础设施服务**
- Eureka Server：服务注册与发现
- Config Server：双实例集中配置管理
- Spring Cloud Bus + RabbitMQ：配置刷新广播
- Gateway：统一入口与路由

✅ **负载均衡**
- 使用 Spring Cloud LoadBalancer
- 通过服务名调用，自动负载均衡
- 增加实例后自动纳入负载

✅ **熔断降级**
- 使用 Resilience4j
- product-service、merchant-service、cart-service、order-service 都已实现完整的熔断降级
- 服务不可用时自动降级，返回空数据或默认值

✅ **路径统一**
- cart-service 的 context-path 改为 /elm，与其他服务一致
- order-service 内部调用路径、Gateway 路由都已更新

✅ **前端适配**
- Nginx 配置添加 Authorization 请求头传递
- 商家积分页面字段映射修复
- 店铺详情页添加降级处理

✅ **业务功能完善**
- 积分抵扣功能完善，下单时冻结积分、扣钱包、最后扣积分
- 订单取消时增加积分退还功能
- 用户领取优惠券问题修复

✅ **Docker 部署**
- 完整的 Docker Compose 配置
- 服务间网络配置
- 健康检查和重启策略

### 7.2 各服务配置状态
| 服务 | 负载均衡 | 熔断降级 | Config Server | 实例数 |
|------|---------|---------|---------------|--------|
| eureka-server | - | - | - | 1 |
| config-server | - | - | - | 2 |
| gateway | ✅ | ❌ | ✅ | 1 |
| user-service | ⚠️ | ❌ | ✅ | 2 |
| merchant-service | ✅ | ✅ | ✅ | 2 |
| product-service | ✅ | ✅ | ✅ | 2 |
| cart-service | ✅ | ✅ | ✅ | 2 |
| order-service | ✅ | ✅ | ✅ | 2 |
| address-service | ❌ | ❌ | ✅ | 1 |
| wallet-service | ❌ | ❌ | ✅ | 1 |
| points-service | ❌ | ❌ | ✅ | 1 |

### 7.3 技术亮点
1. **分层架构**：Gateway + 微服务，职责清晰
2. **配置集中化 + 总线刷新**：Config Server 集中管理，Bus 统一广播刷新
3. **服务解耦**：一个服务挂掉不影响其他服务
4. **优雅降级**：Resilience4j 熔断，返回空数据或默认值
5. **负载均衡**：核心服务双实例，自动负载
6. **容器化**：Docker Compose 一键部署

---

## 8. 未来可扩展方向（可选）
- 引入消息队列做异步解耦（例如：订单创建后异步通知商家）
- 分布式链路追踪（Sleuth + Zipkin）
- 监控与告警（Prometheus + Grafana）
- 分布式事务
- Gateway 层添加熔断降级

---

## 总结
elm-cloud 从单体应用改造为微服务架构，实现了：
- 完整的基础设施（Eureka、双 Config Server、Gateway、RabbitMQ）
- 高可用的核心服务（多实例 + 负载均衡）
- 完善的容错机制（Resilience4j 熔断降级）
- 配置集中化管理与动态刷新（Config Server + Bus + `@RefreshScope`）
- 完整的 Docker 部署方案

通过这次改造，系统具备了企业级的高可用性和容错能力！
