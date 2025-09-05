# 🌊 Ocean Shopping Center - 系統架構設計

## 📋 目錄

- [系統概覽](#系統概覽)
- [高級架構圖](#高級架構圖)  
- [技術棧架構](#技術棧架構)
- [系統組件](#系統組件)
- [數據流架構](#數據流架構)
- [部署架構](#部署架構)
- [安全架構](#安全架構)
- [性能架構](#性能架構)

---

## 🏗️ 系統概覽

Ocean Shopping Center 是一個現代化的多租戶電商平台，採用微服務架構設計理念，支持高並發、高可用性和橫向擴展。系統基於 **前後端分離架構**，使用容器化部署，並配備完整的監控、告警和運維體系。

### 核心特性
- **多租戶支持**: 支援多個商戶獨立運營
- **高性能**: Redis 快取 + 資料庫連接池優化  
- **高可用**: 負載均衡 + 健康檢查 + 故障轉移
- **安全性**: 多層安全防護 + JWT 認證 + SSL/TLS
- **可觀測性**: 完整的監控、日誌、追蹤體系

---

## 🏗️ 高級架構圖

```mermaid
graph TB
    subgraph "Frontend Layer"
        A[React App<br/>19.1.1] --> B[Nginx<br/>Load Balancer]
    end
    
    subgraph "API Gateway & Security"
        B --> C[Spring Boot<br/>API Gateway<br/>3.3.3]
        C --> D[JWT<br/>Authentication]
        C --> E[CORS<br/>Security Headers]
    end
    
    subgraph "Business Logic Layer"
        C --> F[User Service]
        C --> G[Product Service]  
        C --> H[Order Service]
        C --> I[Payment Service]
        C --> J[Cart Service]
        C --> K[Notification Service]
    end
    
    subgraph "Data Layer"
        F --> L[(PostgreSQL<br/>Primary DB<br/>15)]
        G --> L
        H --> L
        I --> L
        J --> M[(Redis<br/>Cache & Session<br/>7)]
        K --> M
        L --> N[(Redis<br/>Replica)]
    end
    
    subgraph "External Services"
        I --> O[Stripe<br/>Payment Gateway]
        H --> P[DHL/FedEx<br/>Logistics]
        K --> Q[Email Service<br/>SMTP]
    end
    
    subgraph "Infrastructure & Monitoring"
        R[Prometheus<br/>Metrics] --> S[Grafana<br/>Dashboards]
        T[ELK Stack<br/>Logs] --> S
        U[Docker<br/>Containers] --> V[Docker Compose<br/>Orchestration]
    end
    
    C --> R
    L --> R
    M --> R
```

---

## 🛠️ 技術棧架構

### 前端技術棧

```mermaid
graph LR
    subgraph "Frontend Stack"
        A[React 19.1.1<br/>Core Framework] --> B[TypeScript<br/>Type Safety]
        B --> C[Tailwind CSS<br/>Styling]
        C --> D[Headless UI<br/>Components]
        D --> E[React Router<br/>Navigation]
        E --> F[Socket.IO<br/>Real-time]
        F --> G[Axios<br/>HTTP Client]
    end
    
    subgraph "State Management"
        H[Context API<br/>Global State] --> I[Custom Hooks<br/>Business Logic]
        I --> J[Local Storage<br/>Persistence]
    end
    
    A --> H
```

### 後端技術棧

```mermaid
graph TD
    subgraph "Backend Stack"
        A[Spring Boot 3.3.3<br/>Core Framework] --> B[Spring Security<br/>Authentication]
        B --> C[Spring Data JPA<br/>ORM Layer]
        C --> D[Spring Data Redis<br/>Cache Layer]
        D --> E[Spring WebSocket<br/>Real-time]
        E --> F[SpringDoc OpenAPI<br/>Documentation]
    end
    
    subgraph "Java Ecosystem"
        G[Java 17<br/>Runtime] --> H[Maven<br/>Build Tool]
        H --> I[Lombok<br/>Code Generation]
        I --> J[Jackson<br/>JSON Processing]
    end
    
    A --> G
```

---

## 🔧 系統組件

### 核心業務組件

```mermaid
graph TB
    subgraph "Authentication & Authorization"
        A[JWT Token Provider] --> B[User Details Service]
        B --> C[Security Filter Chain]
    end
    
    subgraph "Business Services"
        D[User Management] --> E[Product Catalog]
        E --> F[Shopping Cart]  
        F --> G[Order Processing]
        G --> H[Payment Processing]
        H --> I[Shipping Management]
    end
    
    subgraph "Support Services"
        J[Notification Service] --> K[Chat Service]
        K --> L[Coupon Service]
        L --> M[Invoice Service]
    end
    
    subgraph "Data Access Layer"
        N[JPA Repositories] --> O[Connection Pool]
        O --> P[PostgreSQL Database]
        Q[Redis Template] --> R[Redis Cluster]
    end
    
    C --> D
    D --> N
    F --> Q
    H --> Q
```

---

## 🔄 數據流架構

### 用戶請求處理流程

```mermaid
sequenceDiagram
    participant U as User/Browser
    participant N as Nginx
    participant A as Spring Boot API
    participant S as Business Service  
    participant C as Redis Cache
    participant D as PostgreSQL DB
    participant E as External API

    U->>N: HTTP Request
    N->>A: Forward Request
    A->>A: JWT Validation
    A->>S: Business Logic
    
    alt Cache Hit
        S->>C: Check Cache
        C->>S: Return Cached Data
    else Cache Miss
        S->>D: Database Query
        D->>S: Return Data
        S->>C: Update Cache
    end
    
    opt External Service
        S->>E: API Call
        E->>S: Response
    end
    
    S->>A: Service Response
    A->>N: HTTP Response  
    N->>U: Final Response
```

### 實時通信流程

```mermaid
sequenceDiagram
    participant C as Client
    participant W as WebSocket Gateway
    participant S as Spring Service
    participant R as Redis Pub/Sub
    participant D as Database

    C->>W: Connect WebSocket
    W->>S: Establish Session
    S->>R: Subscribe to Channel
    
    Note over C,D: Real-time Event Flow
    
    D->>S: Data Change Event
    S->>R: Publish Message
    R->>W: Broadcast Message
    W->>C: Push to Client
```

---

## 🚀 部署架構

### 開發環境架構

```mermaid
graph TB
    subgraph "Development Environment"
        A[Docker Compose] --> B[Frontend Container<br/>React Dev Server]
        A --> C[Backend Container<br/>Spring Boot]  
        A --> D[PostgreSQL Container<br/>Development DB]
        A --> E[Redis Container<br/>Cache & Session]
        A --> F[pgAdmin Container<br/>DB Management]
        A --> G[Redis Commander<br/>Cache Management]
    end
    
    subgraph "Development Tools"  
        H[Hot Reload] --> B
        I[Maven DevTools] --> C
        J[Volume Mounts] --> C
        J --> B
    end
```

### 生產環境架構

```mermaid
graph TB
    subgraph "Load Balancer Layer"
        A[Internet] --> B[Cloudflare/CDN]
        B --> C[Nginx Load Balancer]
    end
    
    subgraph "Application Layer"
        C --> D[Frontend Instance 1<br/>React + Nginx]
        C --> E[Frontend Instance 2<br/>React + Nginx]
        C --> F[Backend Instance 1<br/>Spring Boot]
        C --> G[Backend Instance 2<br/>Spring Boot]
    end
    
    subgraph "Data Layer"
        F --> H[PostgreSQL Master]
        G --> H
        F --> I[Redis Master]
        G --> I
        H --> J[PostgreSQL Replica]
        I --> K[Redis Replica]
    end
    
    subgraph "Monitoring & Logging"
        L[Prometheus] --> M[Grafana]
        N[ELK Stack] --> M
        O[Jaeger Tracing] --> M
    end
    
    F --> L
    G --> L
    H --> L
    I --> L
```

---

## 🔒 安全架構

### 多層安全防護

```mermaid
graph TB
    subgraph "Network Security"
        A[CloudFlare DDoS] --> B[SSL/TLS Termination]
        B --> C[Firewall Rules]  
    end
    
    subgraph "Application Security"
        C --> D[CORS Policy]
        D --> E[Security Headers]
        E --> F[JWT Authentication]
        F --> G[Role-Based Access Control]
    end
    
    subgraph "Data Security"
        G --> H[Input Validation]
        H --> I[SQL Injection Prevention]
        I --> J[Data Encryption]
        J --> K[Audit Logging]
    end
    
    subgraph "Infrastructure Security"
        L[Container Security] --> M[Secret Management]
        M --> N[Network Isolation]
        N --> O[Regular Security Scans]
    end
```

### 認證授權流程

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth Service
    participant J as JWT Provider
    participant D as Database
    participant R as Redis

    C->>A: Login Request
    A->>D: Validate Credentials
    D->>A: User Data
    A->>J: Generate JWT
    J->>A: Access + Refresh Token
    A->>R: Store Session
    A->>C: Return Tokens
    
    Note over C,R: Subsequent Requests
    
    C->>A: API Request + JWT
    A->>J: Validate Token
    J->>A: Token Claims
    A->>R: Check Session
    R->>A: Session Valid
    A->>C: Authorized Response
```

---

## ⚡ 性能架構

### 快取策略

```mermaid
graph TB
    subgraph "Multi-Level Caching"
        A[Browser Cache<br/>Static Assets] --> B[CDN Cache<br/>Global Distribution]
        B --> C[Nginx Cache<br/>Reverse Proxy]
        C --> D[Application Cache<br/>Spring Cache]
        D --> E[Redis Cache<br/>Distributed]
        E --> F[Database<br/>PostgreSQL]
    end
    
    subgraph "Cache Policies"
        G[TTL-based Expiration] --> H[LRU Eviction]
        H --> I[Write-through Strategy]
        I --> J[Cache Warming]
    end
```

### 數據庫優化

```mermaid
graph LR
    subgraph "Database Performance"
        A[Connection Pooling<br/>HikariCP] --> B[Query Optimization<br/>Indexes & Plans]
        B --> C[Read Replicas<br/>Load Distribution]
        C --> D[Partitioning<br/>Data Sharding]
    end
    
    subgraph "Monitoring"
        E[Query Performance] --> F[Connection Metrics]
        F --> G[Resource Usage]
        G --> H[Alerting Rules]
    end
```

---

## 📊 監控指標

### 關鍵性能指標 (KPIs)

| 指標類型 | 監控項目 | 目標值 | 告警閾值 |
|---------|---------|--------|----------|
| **響應時間** | API P99 延遲 | < 200ms | > 500ms |
| **吞吐量** | 每秒請求數 | > 1000 RPS | < 500 RPS |
| **錯誤率** | HTTP 5xx 錯誤 | < 0.1% | > 1% |
| **可用性** | 服務正常運行時間 | > 99.9% | < 99.5% |
| **資源** | CPU 使用率 | < 70% | > 90% |
| **資源** | Memory 使用率 | < 80% | > 95% |
| **數據庫** | Connection Pool | < 80% | > 95% |
| **快取** | Redis 記憶體使用 | < 75% | > 90% |

---

## 🔧 技術債務與最佳實踐

### 架構原則
1. **單一職責**: 每個服務專注於單一業務領域
2. **依賴反轉**: 面向接口編程，降低耦合度
3. **配置外化**: 環境相關配置外部化管理
4. **故障隔離**: 服務間故障不相互影響
5. **可觀測性**: 全面的監控、日誌、追蹤

### 擴展性考慮
- **水平擴展**: 無狀態設計，支持實例擴展
- **數據分片**: 支持數據庫分片和讀寫分離
- **微服務拆分**: 為未來微服務化做準備
- **API 版本控制**: 支持 API 版本管理和向後兼容

---

## 📖 相關文檔

- [前端架構設計](frontend-architecture.md)
- [後端架構設計](backend-architecture.md)  
- [資料庫設計](database-design.md)
- [部署指南](../deployment/production-deployment.md)
- [監控告警](../monitoring/monitoring-alerting.md)

---

**最後更新**: 2025-09-05  
**版本**: 1.0  
**維護者**: Ocean Shopping Center Team