# Config Server & Spring Cloud Bus 使用指南

## 概述

本项目配置了 Config Server（配置中心）和 Spring Cloud Bus（总线），支持动态配置刷新。

## 已配置的服务

以下服务已经配置为从 Config Server 读取配置：
- `order-service`
- `user-service`

其他服务（`product-service`、`merchant-service`、`cart-service`、`wallet-service`、`points-service`）仍然使用本地配置。

## 配置文件位置

配置文件存放在 `config-server/src/main/resources/config/` 目录：
- `application.yml` - 所有服务的公共配置
- `order-service.yml` - order-service 的配置
- `user-service.yml` - user-service 的配置

## 启动项目

使用 Docker Compose 启动项目：

```bash
docker-compose up -d
```

## 动态刷新配置（核心功能）

### 步骤 1：修改配置文件

在 `config-server/src/main/resources/config/` 目录下修改配置文件（例如 `order-service.yml`）。

### 步骤 2：使用 Bus 刷新所有服务

向 Config Server 发送刷新请求，所有连接到 Config Server 的服务都会被刷新：

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
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
eureka-server
   ↓
config-server
   ↓
[order-service, user-service] ← 使用 Config Server 的服务
   ↓
其他所有服务
```

## 常用 Actuator 端点

### Config Server 端点
- `/actuator/health` - 健康检查
- `/actuator/bus-refresh` - 刷新所有服务配置
- `/actuator/bus-env` - 环境总线刷新

### order-service 端点
- `/actuator/health` - 健康检查
- `/actuator/refresh` - 单独刷新该服务
- `/actuator/circuitbreakers` - 断路器状态

### user-service 端点
- `/actuator/health` - 健康检查
- `/actuator/refresh` - 单独刷新该服务

## 故障排查

### 问题：服务无法连接到 Config Server
- 检查 Config Server 是否正常启动
- 检查 `bootstrap.yml` 中的配置是否正确

### 问题：刷新配置不生效
- 检查是否使用了 `@RefreshScope` 注解
- 检查 RabbitMQ 是否正常运行
- 查看服务日志获取更多信息

### 问题：配置修改后未生效
- 确保刷新请求发送到 Config Server
- 确保服务的 `@RefreshScope` 生效
