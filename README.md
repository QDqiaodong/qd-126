# 企业园区接待室影音设备多楼层归属流转台账系统

## 项目简介

本系统用于企业园区行政统一管理接待室电视、音响、麦克风等影音设备，支持跨楼层、跨接待室调配流转，并维护设备长期调配台账。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Axios
- 后端：Spring Boot 3.3、JDK 17、MyBatis Plus、Redis
- 数据库：MySQL 8.0
- 部署：Docker Compose

## 端口说明

| 服务 | 地址或端口 |
| --- | --- |
| 前端访问地址 | http://localhost:8226 |
| 后端 API 地址 | http://localhost:8326/api |
| MySQL | 127.0.0.1:3526 |
| Redis | 127.0.0.1:6626 |

端口统一维护在根目录 `.env`，示例配置见 `.env.example`。

## 启动方式

```bash
cd /Users/Admin/Desktop/solo-0601/qd-0601/qd-组1/qd-126
docker compose up -d --build
```

## 单独编译验证

```bash
cd backend
mvn compile -q
```

```bash
cd frontend
npm ci
npm run build
```

## Docker 构建说明

前端、后端 Dockerfile 均保留依赖层缓存；Docker Compose 使用 `.env` 中的固定端口，并绑定到 `127.0.0.1`，避免对外暴露和端口漂移。

## 常见问题

- 后端编译失败时先执行 `mvn -version` 检查 JDK，再检查 Lombok、Maven 编译插件和 `pom.xml` 是否被忽略。
- 前端构建失败时优先按实际报错检查 import 路径、导出名、Vite 代理端口和构建期语法。
- 页面中文乱码时检查源码、SQL 初始化脚本、数据库字符集、连接串编码和已有 Docker volume 数据；初始化 SQL 已增加 `SET NAMES utf8mb4;`。
