# Config Server & Spring Cloud Bus 使用指南

## 概述

本项目已经完成 Config Server（配置中心）和 Spring Cloud Bus（总线）的落地，支持集中配置管理、双实例配置中心、Eureka 发现和动态刷新演示。

## 已接入的服务

以下服务已经从 Config Server 读取配置：

- `gateway`
- `order-service`
- `user-service`
- `merchant-service`
- `product-service`
- `cart-service`
- `address-service`
- `points-service`
- `wallet-service`

这些服务都已经接入 Bus，并且 Controller 级别已经统一加上 `@RefreshScope`。

## 配置文件位置

当前中心化配置文件统一放在 `elm-cloud/config/` 目录：

- `application.yml`
- `gateway.yml`
- `order-service.yml`
- `user-service.yml`
- `merchant-service.properties`
- `product-service.properties`
- `cart-service.properties`
- `address-service.properties`
- `points-service.properties`
- `wallet-service.properties`

其中 `order-service.yml` 中的 `demo.config.message` 和 `demo.config.version` 用于动态刷新演示。

## 启动项目

使用 Docker Compose 启动：

```bash
docker compose up -d
```

当前关键基础设施端口如下：

- `config-server-1` -> `8888`
- `config-server-2` -> `8889`
- `eureka-server` -> `8761`
- `gateway` -> `8080`
- `rabbitmq` -> `5672`
- RabbitMQ 管理界面 -> `15672`

## 动态刷新配置

### 步骤 1：修改中心化配置

修改：

```bash
elm-cloud/config/order-service.yml
```

例如修改：

- `demo.config.message`
- `demo.config.version`

### 步骤 2：触发总线广播刷新

向任意一个 Config Server 发送刷新请求：

```bash
curl -X POST http://localhost:8888/actuator/busrefresh
```

或者：

```bash
curl -X POST http://localhost:8889/actuator/busrefresh
```

### 步骤 3：验证刷新是否成功

访问演示接口：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

如果返回值中的 `message` 或 `version` 已经变化，说明刷新成功。

## 推荐脚本

### 基础健康检查

```bash
./scripts/check_config_bus_stack.sh
```

### 自动动态刷新演示

```bash
./scripts/demo_config_bus_refresh.sh
```

### 答辩投屏串讲

```bash
./scripts/defense_config_bus_showcase.sh
```

### 按 task 文档自动验收

```bash
./scripts/verify_task_requirements.sh
```

## 在代码中使用动态刷新

当前项目里采用的是两层做法：

1. 需要展示配置值变化的 Bean 使用 `@RefreshScope`
2. 各微服务 Controller 统一加上 `@RefreshScope`

这意味着：

- Bus 事件广播到服务实例后，Controller 层具备刷新作用域
- 真正需要动态变化的配置值，仍然要来自中心化配置文件

## 常用接口

### Config Server

- `GET /actuator/health`
- `POST /actuator/busrefresh`
- `POST /actuator/bus-env`

### order-service 演示接口

- `GET /elm/api/orders/runtime-config`

## 故障排查

### 服务连不上 Config Server

- 检查两个 Config Server 是否都正常启动
- 检查 Eureka 中是否注册了 `CONFIG-SERVER`
- 检查客户端 `bootstrap.yml` 是否启用了 discovery-first

### 调用了 `busrefresh` 但值没变化

- 检查 RabbitMQ 是否正常运行
- 检查改动是否真的写进了 `elm-cloud/config/` 中的目标服务配置文件
- 检查目标 Bean 或 Controller 是否在刷新作用域内

### 想快速证明链路没问题

直接执行：

```bash
./scripts/demo_config_bus_refresh.sh
```

如果脚本能自动改值、刷新、验证并恢复，就说明当前配置中心和动态刷新主链路工作正常。
