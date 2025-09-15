## 项目介绍

- elm-v2 项目仅包含后端代码，包括与认证相关的全部代码和与业务相关的接口代码和业务对象代码。

- 需要补充后端各层代码，方可通过测试。

## 项目启动

- 当前项目仅依赖 jdk(version>=17) 即可正常运行。
- Build : mvn package
- Run: java -jar target/myapp-1.0.jar （windows上将/换成\）

## 数据库相关

- 当前数据库使用内置h2，无需额外安装数据库。数据库控制台可通过下列地址访问（项目启动后）
  - http://localhost:8080/h2-console/

- 可以通过修改 pom.xml 和 application.properties ，换成 PostgreSQL 或 MySQL 。

- ORM 采用 spring jpa，可以换成 MyBatis 。

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