# 🌊 Ocean Shopping Center

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.1.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-4.9.5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> 現代化多租戶電商平台 - 海洋購物中心網站

## 📋 專案概述

Ocean Shopping Center 是一個全功能的多租戶電商平台，採用現代化的技術棧構建，提供完整的購物體驗、商品管理、訂單處理和支付系統。

### ✨ 核心功能

- 🛍️ **完整購物體驗** - 商品瀏覽、購物車、訂單管理
- 👥 **多租戶支持** - 支援多個商戶獨立管理
- 💳 **安全支付** - 整合 Stripe 支付系統
- 📱 **響應式設計** - 支援桌面和行動裝置
- 🔒 **安全認證** - JWT 認證 + Redis 會話管理
- 📊 **實時監控** - Prometheus + Grafana 監控系統
- 🚀 **高性能** - Redis 快取 + 連接池優化

## 🏗️ 系統架構

### 技術棧概覽

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Frontend    │    │     Backend     │    │    Database     │
│                 │    │                 │    │                 │
│ React 19.1.1    │◄───┤ Spring Boot 3.3.3  │◄───┤ PostgreSQL 15   │
│ TypeScript      │    │ Java 17         │    │ Redis 7         │
│ Tailwind CSS    │    │ Spring Security │    │ pgAdmin         │
│ Socket.IO       │    │ JWT Auth        │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 🎨 前端技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| **React** | 19.1.1 | 主框架 |
| **TypeScript** | 4.9.5 | 型別安全 |
| **Tailwind CSS** | 4.1.12 | 樣式框架 |
| **Headless UI** | 2.2.7 | UI 元件庫 |
| **Heroicons** | 2.2.0 | 圖標庫 |
| **React Router DOM** | 7.8.2 | 路由管理 |
| **React Hook Form** | 7.62.0 | 表單處理 |
| **Axios** | 1.11.0 | HTTP 客戶端 |
| **Socket.IO Client** | 4.8.1 | 即時通信 |

### ⚙️ 後端技術棧

| 技術 | 版本 | 用途 |
|------|------|------|
| **Java** | 17 | 程式語言 |
| **Spring Boot** | 3.3.3 | 應用框架 |
| **Spring Security** | - | 安全框架 |
| **Spring Data JPA** | - | ORM 框架 |
| **Spring Data Redis** | - | 快取管理 |
| **JWT** | 0.12.6 | 認證令牌 |
| **SpringDoc OpenAPI** | 2.6.0 | API 文檔 |
| **Lombok** | 1.18.34 | 程式碼簡化 |
| **Maven** | - | 建置工具 |

### 🗄️ 資料庫層

| 技術 | 版本 | 用途 |
|------|------|------|
| **PostgreSQL** | 15-alpine | 主資料庫 |
| **Redis** | 7-alpine | 快取 & 會話存儲 |
| **pgAdmin** | Latest | 資料庫管理介面 |
| **Redis Commander** | Latest | Redis 管理介面 |

### 🚀 基礎設施

| 技術 | 版本 | 用途 |
|------|------|------|
| **Docker** | - | 容器化 |
| **Docker Compose** | 3.8 | 服務編排 |
| **Nginx** | - | 反向代理 |
| **Prometheus** | - | 監控系統 |
| **Grafana** | - | 監控面板 |
| **Alertmanager** | - | 告警管理 |

## 🚀 快速開始

### 系統要求

- Docker & Docker Compose
- Java 17+ (開發環境)
- Node.js 16+ (開發環境)
- Git

### 1. 複製專案

```bash
git clone https://github.com/JauJyeCH/ocean-shopping-center.git
cd ocean-shopping-center
```

### 2. 環境配置

```bash
# 複製環境變數模板
cp .env.template .env

# 編輯環境變數
nano .env
```

### 3. 啟動服務

```bash
# 啟動基礎服務 (資料庫、快取)
docker-compose up postgres redis -d

# 啟動開發工具 (可選)
docker-compose --profile dev up -d

# 啟動完整應用
docker-compose --profile app up -d
```

### 4. 訪問應用

- **前端應用**: http://localhost:3001
- **後端 API**: http://localhost:8080
- **API 文檔**: http://localhost:8080/swagger-ui.html
- **pgAdmin**: http://localhost:8080 (開發模式)
- **Redis Commander**: http://localhost:8081 (開發模式)

## 📂 專案結構

```
ocean-shopping-center/
├── frontend/                 # React 前端應用
│   ├── src/
│   │   ├── components/      # 可重用元件
│   │   ├── pages/          # 頁面元件
│   │   ├── hooks/          # 自定義 Hooks
│   │   └── services/       # API 服務
│   ├── package.json        # Node.js 依賴
│   └── Dockerfile          # 前端容器配置
├── backend/                  # Spring Boot 後端
│   ├── src/main/java/      # Java 源碼
│   │   └── com/ocean/
│   │       ├── controller/  # REST 控制器
│   │       ├── service/     # 業務邏輯層
│   │       ├── repository/  # 資料存取層
│   │       ├── entity/      # 實體類別
│   │       ├── dto/         # 資料傳輸對象
│   │       └── config/      # 配置類別
│   ├── pom.xml             # Maven 依賴
│   └── Dockerfile          # 後端容器配置
├── database/                 # 資料庫相關
│   ├── schemas/            # 資料庫架構
│   └── migrations/         # 資料庫遷移
├── monitoring/               # 監控配置
│   ├── grafana/            # Grafana 配置
│   └── prometheus.yml      # Prometheus 配置
├── nginx/                   # Nginx 配置
├── scripts/                 # 部署腳本
├── docker-compose.yml      # 開發環境配置
├── docker-compose.prod.yml # 生產環境配置
└── .env.template           # 環境變數模板
```

## 🔧 開發指南

### 前端開發

```bash
cd frontend

# 安裝依賴
npm install

# 啟動開發伺服器
npm start

# 執行測試
npm test

# 建置生產版本
npm run build
```

### 後端開發

```bash
cd backend

# 編譯專案
mvn compile

# 執行測試
mvn test

# 啟動應用
mvn spring-boot:run

# 建置 JAR
mvn package
```

### 資料庫管理

```bash
# 連接 PostgreSQL
docker exec -it ocean_postgres psql -U postgres -d ocean_shopping_center

# 查看 Redis 狀態
docker exec -it ocean_redis redis-cli info

# 備份資料庫
./scripts/backup-restore.sh backup

# 還原資料庫
./scripts/backup-restore.sh restore backup_file.sql
```

## 🔒 安全特性

- **JWT 認證**: 無狀態的安全認證
- **Redis 會話管理**: 分散式會話存儲
- **CORS 保護**: 可配置的跨域請求保護
- **SSL/TLS**: 生產環境強制 HTTPS
- **輸入驗證**: 全面的資料驗證
- **SQL 注入防護**: JPA/Hibernate ORM 保護

## 📊 監控與維護

### 監控系統

- **Prometheus**: 指標收集
- **Grafana**: 視覺化面板
- **Alertmanager**: 告警通知
- **Spring Boot Actuator**: 應用健康檢查

### 關鍵指標

- API 回應時間 (P99 < 200ms)
- 資料庫查詢效能 (P95 < 100ms)
- 快取命中率 (> 95%)
- 錯誤率 (< 0.1%)

## 🚀 部署

### 開發環境部署

```bash
docker-compose --profile app up -d
```

### 生產環境部署

```bash
# 使用生產配置
docker-compose -f docker-compose.prod.yml up -d

# 藍綠部署
./scripts/blue-green-deploy.sh
```

### 健康檢查端點

- **後端健康**: `GET /actuator/health`
- **前端健康**: `GET /health`
- **資料庫連接**: `GET /actuator/health/db`
- **Redis 連接**: `GET /actuator/health/redis`

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/新功能`)
3. 提交變更 (`git commit -am '新增某功能'`)
4. 推送分支 (`git push origin feature/新功能`)
5. 建立 Pull Request

### 開發規範

- 遵循 Google Java Style Guide
- 使用 ESLint + Prettier 格式化前端程式碼
- 編寫單元測試和整合測試
- 確保所有測試通過再提交

## 📄 授權條款

本專案採用 [MIT 授權條款](LICENSE) 開源。

## 🆘 技術支援

如有問題或需要協助，請：

1. 查看 [Wiki 文檔](../../wiki)
2. 搜尋 [Issues](../../issues)
3. 建立新的 [Issue](../../issues/new)

## 🔗 相關連結

- [API 文檔](http://localhost:8080/swagger-ui.html)
- [部署指南](DEPLOYMENT.md)
- [命令參考](COMMANDS.md)
- [代理系統](AGENTS.md)

---

**Ocean Shopping Center** - 打造現代化電商體驗 🌊