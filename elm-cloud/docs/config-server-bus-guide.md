# Config Server & Spring Cloud Bus 使用指南

## 概述

本项目配置了 Config Server（配置中心）和 Spring Cloud Bus（总线），支持动态配置刷新。当前默认以 `native` 模式运行，并通过双实例部署提高演示时的高可用性；如果需要更贴近课件标准，也支持切换到 `git` 模式。

## 已配置的服务

以下服务已经配置为从 Config Server 读取配置：
- `gateway`
- `order-service`
- `user-service`
- `merchant-service`
- `product-service`
- `cart-service`
- `address-service`
- `points-service`
- `wallet-service`

## 配置文件位置

默认 `native` 模式下，配置文件存放在 `elm-cloud/config/` 目录：
- `application.yml` - 所有服务的公共配置
- `gateway.yml` - gateway 的配置
- `order-service.yml` - order-service 的配置
- `user-service.yml` - user-service 的配置
- `merchant-service.properties` - merchant-service 的配置
- `product-service.properties` - product-service 的配置
- `cart-service.properties` - cart-service 的配置
- `address-service.properties` - address-service 的配置
- `points-service.properties` - points-service 的配置
- `wallet-service.properties` - wallet-service 的配置

如果切换到 `git` 模式，Config Server 会从 `CONFIG_GIT_URI` 指向的 Git 仓库读取配置；在本地 Docker Compose 环境下，默认会尝试读取容器内的 `/app/config-repo`。

## 启动项目

使用 Docker Compose 启动项目：

```bash
docker-compose up -d
```

当前会启动两个 Config Server 实例：

- `config-server-1` -> 宿主机端口 `8888`
- `config-server-2` -> 宿主机端口 `8889`

客户端不再写死单一地址，而是通过 Eureka 发现名为 `config-server` 的服务实例。

## native / git 模式切换

### 默认模式：native

不设置额外环境变量时，Config Server 默认以 `native` 模式启动，读取 `elm-cloud/config/`。

### 可选模式：git

如果要切到更接近课件标准的 Git 模式，可以在启动前设置：

```bash
export CONFIG_SERVER_MODE=git
export CONFIG_GIT_URI=https://your-git-host/your-config-repo.git
export CONFIG_GIT_DEFAULT_LABEL=main
docker-compose up -d
```

如果是本地演示，也可以把一个本地 Git 仓库挂载到 `elm-cloud/config-repo/`，然后使用：

```bash
export CONFIG_SERVER_MODE=git
export CONFIG_GIT_URI=file:/app/config-repo
docker-compose up -d
```

## 动态刷新配置（核心功能）

### 步骤 1：修改配置文件

在 `elm-cloud/config/` 目录下修改配置文件（例如 `order-service.yml`）。如果当前是 `git` 模式，则修改 Git 配置仓库中的对应文件并提交。

### 步骤 2：使用 Bus 刷新所有服务

向任意一个 Config Server 实例发送刷新请求，所有连接到 Bus 的服务都会被刷新：

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
```

或者发给第二个实例：

```bash
curl -X POST http://localhost:8889/actuator/bus-refresh
```

或者使用 BusEnv 刷新：

```bash
curl -X POST http://localhost:8888/actuator/bus-env
```

### 步骤 3：为单个服务刷新（可选）

如果只想刷新特定的服务，可以使用：

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh/order-service
```

## 验证刷新是否成功

可以通过查看服务的日志或检查 Actuator 端点来验证：

```bash
curl http://localhost:8084/elm/actuator/health
```

也可以直接访问已经接入 `@RefreshScope` 的演示接口：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

修改 `elm-cloud/config/order-service.yml` 中的 `demo.config.message` 或 `demo.config.version` 后，再调用：

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
```

再次请求 `runtime-config` 即可看到新值。

## 在代码中使用动态配置

如果要让某个 Bean 支持动态刷新，需要使用 `@RefreshScope` 注解：

```java
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class MyController {
    
    @Value("${my.config.property:default}")
    private String configValue;
    
    @GetMapping("/config")
    public String getConfig() {
        return configValue;
    }
}
```

## 访问 RabbitMQ 管理界面

- URL: http://localhost:15672
- 用户名: guest
- 密码: guest

## 服务依赖关系图

```
rabbitmq
    ↓
[config-server-1, config-server-2]
    ↓  (注册到 Eureka，服务名统一为 config-server)
[gateway, order-service, user-service, merchant-service, product-service, cart-service, address-service, points-service, wallet-service]
```

## 常用 Actuator 端点

### Config Server 端点
- `/actuator/health` - 健康检查（两个实例都可访问）
- `/actuator/bus-refresh` - 刷新所有服务配置
- `/actuator/bus-env` - 环境总线刷新

### order-service 端点
- `/actuator/health` - 健康检查
- `/actuator/refresh` - 单独刷新该服务
- `/actuator/circuitbreakers` - 断路器状态
- `/api/orders/runtime-config` - 热刷新演示接口

### user-service 端点
- `/actuator/health` - 健康检查
- `/actuator/refresh` - 单独刷新该服务

## 故障排查

### 问题：服务无法连接到 Config Server
- 检查两个 Config Server 是否正常启动并已注册到 Eureka
- 检查 `bootstrap.yml` 中是否启用了 `spring.cloud.config.discovery.enabled=true`
- 检查 `bootstrap.yml` 中的 Eureka 地址是否正确

### 问题：刷新配置不生效
- 检查是否使用了 `@RefreshScope` 注解
- 检查 RabbitMQ 是否正常运行
- 查看服务日志获取更多信息

### 问题：配置修改后未生效
- 确保刷新请求发送到了任意一个 Config Server 实例
- 确保服务的 `@RefreshScope` 生效
