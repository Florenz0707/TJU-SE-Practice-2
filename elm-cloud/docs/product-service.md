# product-service 微服务详情

## 1. 功能概述
`product-service` (端口: 8091) 主要负责管理核心菜品（Food/Product）相关的维护与展示。其主要职责为：
- 维护所有商家的菜单列表及菜品实体的详细信息。
- 处理新菜品的添加、信息更新以及供前端店铺页面的分类展示。

## 2. 数据库与表结构
- 数据库连接：使用业务共享或独立的菜品库（如 `elm_product` 或 `elm_catalog`）。
- 核心实体与表：
  - `Food`（或 `product`）表包含：`id`, `food_name` (品名), `food_price` (单价), `food_img` (图片), `food_explain` (介绍), 和 `business_id` (所属商家ID)。
  - 注意：由于业务被独立化，此表直接记录所属的 `business_id`，而不再用 Hibernate 去关联完整的 `Business` 获取。这种打平化结构减小了关联的开销并适应了新微服务规范。

## 3. 提供的接口

### 3.1 外部暴露接口 (外部通过 Gateway 调用)
前端调用的前缀主要为 `/elm/api/foods` 以及为了兼容老单体的端点：
- **GET /api/foods**：支持按 `businessId` 筛选参数（`?business=xxx`）获取当前商家所有可点菜品列表。
- **GET /api/foods/{id}**：获取指定菜品的详细图文和价格等详情信息。
- **POST /api/foods**：允许拥有商家权限的用户在后台添加新的专属菜品项目。
- 兼容的单体端点：`RequestMapping("/FoodController/listFoodByBusinessId")`，继续兼容以前 Vue 老版以表单提交 `food.businessId` 的方式来查阅菜单的需求。

### 3.2 内部接口
由于菜品本身数据相对独立，主要提供简单的基础信息查询（例如查询价格等）供后续的 `order-service` 或 `cart-service` 在处理订单核算时使用，通常通过内部端点或是数据库级别的镜像/快照冗余来实现，当前阶段业务依赖前端 Gateway 进行一次性拉取并组装。

## 4. 迁移细节与遇到的问题
- **剥离商家与菜品的数据强耦合**：老系统 `elmboot` 中的 `Food` 对象与 `Business` 对象互相具有很深的从属（一对多）联系。在新版微服务迁移中，我们将菜品的实体中与商家的关系变成了一个简单的 `Long businessId` 外键引用。并通过内置静态类 (`BusinessDto`) 加上 `@Transient` 的形式临时模拟了原有一对多结构所需的嵌套JSON层级，进而无缝适配了前端的展示要求。
- **接口迁移融合**：在重构的 `FoodController` 中，结合了基于表单提交的查询和针对 OpenAPI 标准的 REST JSON 增改查接口（`@RequestBody` 形式），从而在保障前后台双版本的兼容能力时，移除或替换了一些深奥、冗余的 JPA 映射操作。
