# --- 第一阶段：构建 (Builder) ---
# 使用 Node 20 (兼容 Vite 新版本)
FROM node:20-alpine AS builder

WORKDIR /app

# 1. 先复制依赖描述文件，利用 Docker 缓存加速 npm install
COPY package.json package-lock.json ./

# 2. 安装依赖
RUN npm install

# 3. 复制所有源代码
COPY . .

# 4. 执行构建 (对应 package.json 中的 "build": "vite build")
# 构建结果默认在 /app/dist 目录下
RUN npm run build

# --- 第二阶段：运行 (Runner) ---
# 使用 Nginx 稳定版
FROM nginx:stable-alpine

# 1. 从构建阶段复制打包好的静态资源到 Nginx 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 2. 复制自定义的 Nginx 配置文件 (解决路由 404 和反向代理问题)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 3. 暴露端口
EXPOSE 80

# 4. 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]