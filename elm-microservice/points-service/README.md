# points-service

积分微服务（阶段2拆分试点）。

## 运行

1. 配置环境变量（示例）
   - `DB_URL=jdbc:mysql://localhost:3306/elm_points?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`
   - `DB_USERNAME=root`
   - `DB_PASSWORD=root`
   - `INTERNAL_SERVICE_TOKEN=internal-service-secret-token-2024`
2. 启动
   - `mvn spring-boot:run`

默认地址：

- `http://localhost:8081/elm`

## 关键接口

- 内部积分接口：`/elm/api/inner/points/**`
- 用户积分接口：`/elm/api/points/**`
- 管理员积分规则接口：`/elm/api/points/admin/rules/**`
