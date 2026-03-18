# 天津大学软件学院-软件工程（系列）实践项目

## 项目结构

- `elm-v2.0/` - 后端服务（Spring Boot + MySQL）
- `elm-frontend/` - 前端服务（Vue 3 + Vite）
- `docker-compose.yml` - Docker编排配置

## 快速开始

### 环境变量配置

1. 复制环境变量模板：

```bash
cp elm-v2.0/.env.example elm-v2.0/.env
```

2. 编辑`.env`文件，配置数据库连接信息：

```
DB_URL=jdbc:mysql://localhost:3306/elm?...
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

## **部署方式：**

### 选项 1：使用Docker Compose（推荐）

启动所有服务（前端、后端、MySQL）：

```bash
docker-compose up -d
```

访问应用：

- 前端：http://localhost
- 后端API：http://localhost:8080/elm
- Swagger文档：http://localhost:8080/elm/swagger-ui/index.html

### 选项 2：在 Docker 中构建所有内容（适合CI/CD或无本地环境）

**如果本地没有开发环境，或者想让 Docker 处理所有编译工作，请运行以下命令：**

```
docker-compose --profile build up --build -d

```

此命令将激活 `docker-compose.yml` 文件中的 `build` 配置，该配置会使用包含多阶段构建的 Dockerfile 来从源码编译并运行此项目。

### 选项 2：使用本地构建的构件（适合本地开发，速度更快）

**如果本地有jdk-21或者npm环境，可以本地构建对应的部分，使用此方法来快速启动。支持仅本地构建前端或后端**

#### 第 1 步：在本地构建项目

- **后端**：进入 `elm-v2.0` 目录并运行：

  ```
  mvn package

  ```

  这将在 `elm-v2.0/target/` 目录下生成 `elm-1.0.jar` 文件。

- **前端**：进入 `elm-frontend` 目录并运行：

  ```
  npm install && npm run build

  ```

  这将在 `elm-frontend/dist/` 目录下生成静态资源文件。

#### 第 2 步：使用 Docker Compose 启动

**构件准备好后，在项目根目录运行以下命令：**

```
# 全本地构建
docker-compose --profile local up --build -d

# 仅本地构建后端，使用docker构建前端
docker-compose --profile build-frontend up --build -d

# 仅本地构建前端，使用docker构建后端
docker-compose --profile build-backend up --build

```

## 测试数据：

可使用根目录下`添加数据.apifox-cli.json`，导入Apifox中添加测试用数据。

admin用户（密码admin）具有顾客，商家，管理三个身份

user用户（密码password）具有顾客身份

## 前端说明：

前端实现了桌面端与移动端两套UI，均实现了顾客，商家，管理三种角色的页面。桌面端可通过顶部导航栏切换，移动端可通过我的页面切换。
