# 阶段 1：构建 Vue 项目
FROM node:14-slim as build

WORKDIR /app
RUN npm config set registry https://registry.npmmirror.com
COPY package*.json ./
RUN npm install -g cnpm --registry=https://registry.npmmirror.com && cnpm install
COPY . .
RUN npm run build

# 阶段 2：Nginx 部署
FROM nginx:1.23-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
