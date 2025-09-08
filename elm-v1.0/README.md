# elm 饿了么 V1.0

## 1. 内容
- elmclient 前端代码，基于 VUE 2
- elmboot 后端代码，基于 Spring Boot
- elm.sql 数据库初始化脚本

## 2. 数据库设置

- 首先，需要有一个 MySQL Server，为了协同开发方便调试，建议租用一台云服务器来安装 MySQL 。
- 将数据库初始化脚本 elm.sql 导入到数据库中，并设置好访问的用户和权限。

## 3. 后端项目 elmboot 部署运行 
- 首先需要下载安装 [JDK](https://www.oracle.com/cn/java/technologies/downloads/ "JavaSE官网") 和 项目管理工具 [Maven](https://maven.apache.org/ "Maven官网") 。 
- 修改 `src/main/resources/application.properties` 文件，填写连接数据库需要的正确的地址、用户名和密码。
- 运行打包命令 `mvn package` ，生成 `target/elmboot-0.0.1-SNAPSHOT.jar` 。这是”胖jar包“，一个jar包就包含后端项目运行所需的全部文件，可以拷贝到任何一台仅安装了 JVM 的机器上运行。
- 运行命令 `java -jar elmboot-0.0.1-SNAPSHOT.jar` ，启动后端项目，保持字符窗口不被关闭即可提供服务。
- 在浏览器上输入 [http://localhost:8080/elm/UserController/getUserByIdByPass?userId=11111111111&password=123](http://localhost:8080/elm/UserController/getUserByIdByPass?userId=11111111111&password=123 )，如果看到该用户的详细信息，说明后端项目运行正常。（多么不安全的服务）

## 4. 测试后端项目 
- 建议使用 [Postman](https://www.postman.com/ "Postman官网")、 [Apifox](https://apifox.com/ "Apifox官网") 之类的工具，模拟 HTTP 请求，对后端进行测试。

## 5. 前端项目 elmclient 部署运行
- 下载安装 [Node.js](https://nodejs.org "Node.js官网")，会安装 node 及 npm 命令。
- 修改 npm 源为国内源（国外源太慢）: `npm config set registry https://registry.npmmirror.com`
- 安装 npm 的国内替代品 cnpm ：`npm install -g cnpm --registry=https://registry.npmmirror.com`
- 安装依赖包，在 elmclient 目录下执行 `cnpm install` ，此过程会生成 node_modules 目录并将依赖包放在此目录下。后期如果出现依赖问题，可以将此目录删除后重新执行本命令。
- 提供测试用的前端服务： `npm run serve` ，编译通过后会提示前端服务运行在 localhost 的 8081 端口 ，在浏览器中输入 [ http://localhost:8081/ ](http://localhost:8081/ )
- 生成部署版本：`npm run build`，编译后的结果放在 dist 目录下，此目录即可部署到正式的 Web Server (如 Nginx) 上。

## 6. 前后端部署在同一个服务中的方法（为运行方便，非主流做法，仅供参考，不做推荐）
- 在前端项目的 `src/main.js` 中，修改访问后端API地址,去掉 `elm/` ，然后编译前端项目，将前端项目编译生成的 dist 目录，放在后端项目的 `src/main/resources/` 目录下， 将 `dist` 改名为 `static` 。注意，正式部署时，需要在前端代码中设置好正式的后端服务的地址，不能再是 localhost 了，localhost 仅调试时使用。
- 在后端项目的 `src/main/resources/application.properties` 文件中，修改后端 server 的地址，将 `/elm` 改为 `/` 。再编译后端项目，生成胖jar包。
- 运行胖jar包，这时访问 [ http://localhost:8080/ ](http://localhost:8080/ ) 就是前端页面了，前端页面和后端服务API共存于8080端口的同一服务内。这样做还可以避免出现跨域访问的问题。