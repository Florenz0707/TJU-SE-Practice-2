# 1. 构建阶段
FROM node:22-alpine AS build
WORKDIR /app
COPY . .
RUN npm install && npm run build

# 2. 部署阶段
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
