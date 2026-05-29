# Multi-stage Dockerfile: build the Vite app and serve with nginx
FROM node:18-alpine as builder
WORKDIR /app
COPY package.json package-lock.json* ./
COPY . .
ARG VITE_API_BASE=/api
ENV VITE_API_BASE=${VITE_API_BASE}
RUN rm -rf node_modules package-lock.json && npm config set registry https://registry.npmmirror.com && npm install --legacy-peer-deps --silent
RUN npm run build

FROM nginx:stable-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
