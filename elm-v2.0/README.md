## 项目介绍

- elm-v2.0 后端服务，基于Spring Boot 3.5.4 + MySQL 8.0
- 包含认证、业务接口、积分系统、优惠券系统、钱包系统等完整功能
- 前端代码位于 `../elm-frontend` 目录

## 快速开始

### Docker部署（推荐）

在项目根目录执行：

```bash
docker-compose up -d
```

### 本地开发

1. 配置MySQL数据库：

```sql
CREATE DATABASE elm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量：

```bash
cp .env.example .env
# 编辑.env文件，设置数据库连接
```

3. 启动应用：

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn package
java -jar target/elm-1.0.jar
```

## 环境变量

通过`.env`文件配置：

- `DB_URL` - 数据库连接URL
- `DB_USERNAME` - 数据库用户名
- `DB_PASSWORD` - 数据库密码

## 数据库

- 使用MySQL 8.0
- Hibernate自动创建表结构（ddl-auto=update）
- 无需手动执行SQL脚本

## 接口及测试

- 项目接口文档，可使用下列地址访问（项目启动后）
  - http://localhost:8080/swagger-ui/index.html
- 在apifox上的公开项目访问地址
  - https://tjusep.apifox.cn/
- 测试接口（方法一：使用apifox cli）
  - 安装node （可从 https://nodejs.org 下载安装）
  - 安装apifox （ npm install -g apifox-cli ，如网络不畅，可先装cnpm，再 cnpm install -g apifox-cli ）
  - 修改测试文件”测试正常创建订单流程.apifox-cli.json“，将其中的 localhost 换为你自己的IP地址（用ipconfig/ifconfig可查）
  - 启动项目，并执行：apifox run 测试正常创建订单流程.apifox-cli.json

- 测试接口（方法二：登录apifox协作帐号）
  - 下载apifox桌面版（可从 https://apifox.com 下载）
  - 使用手机号 18522610428 密码 Tju1895se 登录
  - 在个人空间中找到 ”天津大学软件工程实践项目“ ，在”自动化测试“中可以完成”测试正常创建订单流程“
