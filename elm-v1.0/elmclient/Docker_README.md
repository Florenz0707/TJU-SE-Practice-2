# 📘 前端（elmclient）容器部署说明

## 1. 开发模式（热更新调试用）

在容器中跑 `npm run serve`，适合开发调试。

```bash
docker-compose up
```

浏览器访问：

```
http://localhost:8081
```

---

## 2. 生产模式（部署上线用）

用 Nginx 托管 `npm run build` 生成的静态文件。

```bash
docker-compose -f docker-compose.yml up -d --build
```

浏览器访问：

```
http://localhost/
```

---

## 3. 选择使用模式

* **开发调试** → 用 ​**开发模式容器**​，代码实时更新。
* **上线部署** → 用 ​**生产模式容器**​，打包成静态文件，由 Nginx 提供服务，性能更好。

