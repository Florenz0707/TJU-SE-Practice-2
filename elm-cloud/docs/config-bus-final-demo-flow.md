# Config + Bus 最终演示流程

这份流程只描述当前仓库里已经落地并验证通过的最终实现。

## 1. 演示前开场

推荐开场：

> 我现在展示的是项目里已经落地的 Config + Bus 最终版本。当前有双 Config Server、Eureka discovery-first、RabbitMQ 广播刷新、集中配置目录，以及 Controller 级别的 `@RefreshScope` 刷新能力。

## 2. 推荐演示顺序

### 第一步：展示双实例 Config Server

展示 `elm-cloud/docker-compose.yml` 中有两个配置中心实例：

- `config-server-1`
- `config-server-2`

说明：

- 两个实例都会注册到 Eureka
- 客户端发现的是统一的 `config-server` 服务名
- 这样配置中心本身不再是单点

### 第二步：展示 discovery-first

打开任意一个业务服务的 `bootstrap.yml`，展示：

- `spring.cloud.config.discovery.enabled=true`
- `spring.cloud.config.discovery.service-id=config-server`

答辩口径：

> 现在客户端不再依赖固定地址，而是通过注册中心发现配置中心实例。

### 第三步：展示中心化配置目录

打开 `elm-cloud/config/`，说明：

- 各服务配置已经统一收口到这里
- `order-service.yml` 里有演示用的 `demo.config.message` 和 `demo.config.version`

答辩口径：

> 现在配置不是散在各个服务里，而是由 Config Server 统一读取和下发。

### 第四步：展示刷新前的运行时配置

访问：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

你会看到类似结果：

```json
{"service":"order-service","message":"order-service remote config ready","version":"v1"}
```

### 第五步：修改配置

修改 `elm-cloud/config/order-service.yml`：

- 修改 `demo.config.message`
- 或修改 `demo.config.version`

### 第六步：触发 Bus 广播刷新

调用任意一个 Config Server 的刷新入口：

```bash
curl -X POST http://localhost:8888/actuator/busrefresh
```

或者：

```bash
curl -X POST http://localhost:8889/actuator/busrefresh
```

答辩口径：

> 这一步会把刷新事件发到 RabbitMQ，再由总线广播给所有接入的服务实例。

### 第七步：再次访问演示接口

再次执行：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

展示返回值已经变化。

答辩口径：

> 这一步证明的不只是 Bus 依赖存在，而是业务侧确实已经观察到了刷新结果。

## 3. 如果你想更稳地演示

可以直接使用仓库里已经准备好的自动化脚本：

### 一键串讲脚本

```bash
./scripts/defense_config_bus_showcase.sh
```

这个脚本会按答辩顺序自动展示：

1. 关键容器状态
2. discovery-first 证据
3. 中心化配置目录和运行时接口
4. 修改配置、触发 `busrefresh`、展示刷新结果

### 自动刷新演示脚本

```bash
./scripts/demo_config_bus_refresh.sh
```

这个脚本会自动修改配置、调用 `busrefresh`、校验返回值变化，并在结束后恢复原值。

## 4. 最稳的收口句

> 我现在的实现已经把 Config Server 双实例、discovery-first、集中配置、Bus 广播刷新和 Controller 级 `@RefreshScope` 都补齐了，而且可以通过运行时接口直接证明配置值在刷新后发生变化。
