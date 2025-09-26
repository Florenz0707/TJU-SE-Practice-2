## 天津大学软件学院-软件工程（系列）实践项目

#### **部署方式：**

```bash
# 启动
docker-compose up -d --build
# 退出
docker-compose down
```

#### 测试数据：

可使用根目录下`添加数据.apifox-cli.json`，导入Apifox中添加测试用数据。

admin用户（密码admin）具有顾客，商家，管理三个身份

user用户（密码password）具有顾客身份

#### 前端说明：

前端实现了桌面端与移动端两套UI，均实现了顾客，商家，管理三种角色的页面。桌面端可通过顶部导航栏切换，移动端可通过我的页面切换。
