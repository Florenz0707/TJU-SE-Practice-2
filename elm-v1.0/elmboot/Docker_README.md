## 📄 后端容器部署

```markdown
# elmboot - Spring Boot 后端服务

这是一个基于 **Spring Boot + MySQL** 的后端项目，支持在 **云端容器** 中开发和部署。  
数据库运行在独立服务器，本项目容器只负责后端服务。

---

## 🚀 环境要求

- **JDK 11**（开发模式使用）
- **Maven 3.8+**
- **Docker 20+ / Docker Compose 2+**
- **MySQL 8.0**（运行在另一台服务器）

---

## ⚙️ 项目结构

```

elmboot/
├── src/                       # 源代码
├── target/                    # Maven 打包输出
├── pom.xml                    # Maven 配置文件
├── Dockerfile                 # 生产环境镜像构建
├── docker-compose.yml          # 生产部署配置
├── docker-compose.override.yml # 开发模式配置
└── README.md                   # 项目说明

## 🛠 开发模式（热加载）

在云端或本地直接运行源码，容器内会使用 Maven 编译并启动 Spring Boot。

```bash
docker-compose up
```

特点：

* 代码挂载到容器，修改后可实时生效。
* 配合 `spring-boot-devtools` 支持自动重启。

## 📦 生产部署

1. **构建并运行容器**
   ```bash
   docker-compose -f docker-compose.yml up -d --build
   ```
2. **查看日志**
   ```bash
   docker logs -f elmboot-backend
   ```
3. **访问服务**
   ```
   http://<CLOUD_SERVER_IP>:8080
   ```

## ⚡ 常用命令

### 开发模式

```bash
docker-compose up              # 前台运行
docker-compose down            # 停止并移除容器
```

### 生产模式

```bash
docker-compose -f docker-compose.yml up -d --build  # 构建+后台运行
docker ps                                          # 查看容器状态
docker logs -f elmboot-backend                     # 查看日志
docker stop elmboot-backend                        # 停止服务
```

## ✅ 快速开始

```bash
# 开发模式（云端热加载）
docker-compose up

# 生产模式（打包运行）
docker-compose -f docker-compose.yml up -d --build
```
