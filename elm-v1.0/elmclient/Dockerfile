# Stage 1: Build Vue project
FROM node:16-alpine as build

WORKDIR /app

# Set npm registry
RUN npm config set registry https://registry.npmmirror.com

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install -g cnpm --registry=https://registry.npmmirror.com && \
    cnpm install

# Copy source code
COPY . .

# Build the project
RUN npm run build

# Stage 2: Nginx deployment
FROM nginx:1.25-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built files
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration if needed
# COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]