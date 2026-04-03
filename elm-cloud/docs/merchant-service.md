# merchant-service 微服务详情

## 1. 功能概述
`merchant-service` 负责商家信息的管理与店铺审核流程。其主要职责为：
- 维护商家的基本信息（店铺名、描述、配送费等）。
- 处理商家入驻申请（Merchant Application）以及店铺上架申请（Business Application）。
- 供查询可用商家列表及商家详情，支持外卖平台核心搜索和浏览功能。

## 2. 数据库与表结构
- 数据库连接：使用 `elm_merchant` 集群或拆分隔离的商家业务表。
- 核心实体与表：
  - `Business` 表：包含 `businessName`, `businessAddress`, `businessExplain`, `starPrice`, `deliveryPrice` 等商家基础展示数据。
  - `BusinessApplication` 表：记录店铺经营申请，包含状态控制属性如 `applicationState` 和说明 `applicationExplain`。
  - `MerchantApplication` 表：记录商家个人的入驻资格申请。

## 3. 提供的接口

### 3.1 外部暴露接口 (外部通过 Gateway 调用)
前端调用的前缀主要为 `/elm/api/businesses` 和 `/elm/api/applications`：
- **GET /api/businesses**：获取所有已过审或满足规则的商家列表。
- **GET /api/businesses/{id}**：根据获取商家的详细信息（被点餐页面或者查看商家信息的流程依赖）。
- **GET /api/businesses/my**：以商家身份获取当前认证用户开设的店铺列表。
- **POST /api/applications/merchant**：商家提交入驻身份验证和资质申请流程。
- **POST /api/applications/business**：针对店铺本体建立信息的独立开启申请。

### 3.2 内部接口
以 `/internal/**` 为主，供其他核心微服务（如 order 等需要在创建订单时校验商家）发起查询以整合商家的全量属性。
- **GET /internal/businesses/{id}**：内部系统级请求，返回商家全集及关键状态校验，不需要进行用户级别的身份鉴权。

## 4. 迁移细节与遇到的问题
- **单体服务拆分与 API 规范化**：从老式 elmboot 拆分出相关类时，老版前端采用的是类表单提交形式 (`BusinessController` / `listBusinessByOrderTypeId`)。新版微服务既保持了老式的控制器向下兼容，同时也拓展了针对新版前端更RESTful规范的 `/api/businesses` JSON 接口。
- **关联实体的解耦（User循环防范）**：原本 JPA 中的级联极其依赖 `User` 进行鉴权或是 `businessOwner` 对象。将其切出之后，我们更喜欢直接保留 `business_owner_id` 形式防范高频率与 User 服务相互RPC并保持低耦合调用。仅在内部或返回阶段用简单的对象封装即可。