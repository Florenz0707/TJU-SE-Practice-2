# Config + Bus 最终演示流程

这份流程按“更接近课程标准、但仍保证本地演示稳定”的目标设计。

## 1. 演示前你要先说明的口径

推荐开场：

> 我现在这套实现默认采用 native 模式，保证本地 Docker 演示稳定；同时已经补上了双 Config Server、Eureka discovery-first 和 git profile 切换入口。所以它既能稳定演示，也已经具备向课件标准方案靠拢的结构。

## 2. 推荐演示顺序

### 第一步：展示双实例 Config Server

展示 `elm-cloud/docker-compose.yml` 中有两个配置中心实例：

- `config-server-1`
- `config-server-2`

然后说明：

- 两个实例都会注册到 Eureka
- 客户端看到的是统一的 `config-server` 服务名
- 这比早期“直连单节点地址”的方案更接近课程标准

### 第二步：展示客户端 discovery-first

打开任意一个业务服务的 `bootstrap.yml`，展示：

- `spring.cloud.config.discovery.enabled=true`
- `spring.cloud.config.discovery.service-id=config-server`

答辩口径：

> 这样客户端不依赖固定节点地址，而是通过注册中心发现配置中心实例，更适合集群化部署。

### 第三步：展示 Config Server 支持 native / git 双模式

打开 `config-server/src/main/resources/application.yml`，说明：

- 默认 `CONFIG_SERVER_MODE=native`
- 也支持 `git` profile
- `git` 模式下通过 `CONFIG_GIT_URI` 指定仓库地址

答辩口径：

> 默认保留 native，是为了降低本地演示的不确定性；如果老师要求更接近课件标准，只要切到 git 模式即可。

### 第四步：展示运行中的热刷新接口

访问：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

你会看到类似结果：

```json
{"service":"order-service","message":"order-service remote config ready","version":"v1"}
```

### 第五步：修改配置

如果当前是 native 模式：

- 修改 `elm-cloud/config/order-service.yml`
- 调整 `demo.config.message` 或 `demo.config.version`

如果当前是 git 模式：

- 修改 Git 配置仓库中的 `order-service.yml`
- 提交这次配置变更

### 第六步：触发 Bus 广播刷新

向任意一个 Config Server 实例发送刷新请求：

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
```

或者：

```bash
curl -X POST http://localhost:8889/actuator/bus-refresh
```

答辩口径：

> 这里无论打到哪个 Config Server，本质上都会把刷新事件发到 RabbitMQ，再由 Bus 广播到所有接入的服务实例。

### 第七步：再次访问演示接口

再次执行：

```bash
curl http://localhost:8080/elm/api/orders/runtime-config
```

展示返回值已经变化。

答辩口径：

> 这一步证明的不只是 Bus 基础设施存在，而是业务侧确实能观察到配置热更新效果。

## 3. 如果老师要求你演示 Git 模式

### 方式 A：远程 Git 仓库

```bash
export CONFIG_SERVER_MODE=git
export CONFIG_GIT_URI=https://your-git-host/your-config-repo.git
export CONFIG_GIT_DEFAULT_LABEL=main
cd elm-cloud
docker compose up -d --build
```

### 方式 B：本地 Git 仓库

先准备一个本地配置仓库，并保证它本身是 Git 仓库。

然后：

```bash
export CONFIG_SERVER_MODE=git
export CONFIG_GIT_URI=file:/app/config-repo
cd elm-cloud
docker compose up -d --build
```

`docker-compose.yml` 已经把 `./config-repo` 挂载到了容器内的 `/app/config-repo`。

## 4. 最稳的一句话总结

> 我现在的实现已经把 Config Server 双实例、discovery-first、Bus 广播刷新和业务热刷新演示点都补齐了。默认保留 native 模式是为了本地演示稳定，但如果需要更贴近课件标准，也可以直接切到 git 模式。