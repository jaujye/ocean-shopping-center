# Ocean Shopping Center - Production Deployment Guide

> **Version**: 1.0  
> **Last Updated**: 2025-09-05  
> **Target Environment**: Production  
> **Performance Target**: 10,000+ concurrent users

## 🚀 Quick Start

### Prerequisites Checklist
- [ ] Docker and Docker Compose installed
- [ ] PostgreSQL 15+ database server
- [ ] Redis 7+ cache server  
- [ ] SSL certificates obtained
- [ ] Domain names configured
- [ ] Environment variables configured
- [ ] Load testing completed
- [ ] Security assessment passed

### One-Command Production Deployment
```bash
# Clone and deploy
git clone https://github.com/your-org/ocean-shopping-center.git
cd ocean-shopping-center
cp .env.template .env.production
# Edit .env.production with your values
docker-compose -f docker-compose.prod.yml up -d
```

## 📋 Deployment Environments

### Development Environment
```bash
# Local development with live reload
docker-compose --profile dev up -d postgres redis
npm run dev:backend & npm run dev:frontend
```

### Staging Environment
```bash
# Staging with production-like setup
docker-compose -f docker-compose.yml --profile app up -d
```

### Production Environment  
```bash
# Full production deployment with monitoring
docker-compose -f docker-compose.prod.yml up -d
```

## 🏗️ Infrastructure Architecture

### Production Architecture Diagram
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Load Balancer │────│  Nginx Reverse   │────│   Application   │
│    (External)   │    │      Proxy       │    │    Frontend     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                       ┌──────────────────┐    ┌─────────────────┐
                       │   Spring Boot    │────│   PostgreSQL    │
                       │     Backend      │    │    Database     │
                       └──────────────────┘    └─────────────────┘
                                │
                       ┌──────────────────┐    ┌─────────────────┐
                       │   Redis Cache    │    │   Monitoring    │
                       │   & Sessions     │    │     Stack       │
                       └──────────────────┘    └─────────────────┘
```

### Component Specifications

#### Frontend (React + Nginx)
- **Technology**: React 19, TypeScript, Tailwind CSS
- **Web Server**: Nginx with performance optimizations  
- **Container**: Alpine Linux, multi-stage build
- **Resources**: 512MB RAM, 0.5 CPU cores
- **Features**: Gzip compression, caching, security headers

#### Backend (Spring Boot + JRE 17)
- **Technology**: Spring Boot 3.3.3, Java 17, Maven
- **Container**: Eclipse Temurin JRE 17 Alpine
- **Resources**: 4GB RAM, 2 CPU cores (production)
- **Features**: Connection pooling, caching, monitoring endpoints

#### Database (PostgreSQL 15)
- **Version**: PostgreSQL 15 Alpine
- **Resources**: 2GB RAM, 1 CPU core minimum
- **Features**: Connection pooling, automated backups, performance tuning
- **Storage**: Persistent volumes with backup retention

#### Cache (Redis 7)
- **Configuration**: Master-replica setup in production
- **Resources**: 1GB RAM, 0.5 CPU cores
- **Features**: Session storage, application caching, real-time data

## 🔧 Configuration Management

### Environment Variables

#### Required Production Variables
```bash
# Application Configuration
APP_VERSION=1.0.0
NODE_ENV=production
SPRING_PROFILES_ACTIVE=production

# Database Configuration  
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=ocean_shopping_center
POSTGRES_USER=ocean_user
POSTGRES_PASSWORD=secure_db_password_2025

# Redis Configuration
REDIS_HOST=redis-master
REDIS_PORT=6379
REDIS_PASSWORD=secure_redis_password_2025
REDIS_DATABASE=0

# Security Configuration
JWT_SECRET=ultra_secure_jwt_secret_key_2025_production
JWT_EXPIRATION=604800000
BCRYPT_ROUNDS=12

# API URLs
REACT_APP_API_URL=https://api.oceanshoppingcenter.com
REACT_APP_WS_URL=wss://api.oceanshoppingcenter.com
CORS_ALLOWED_ORIGINS=https://oceanshoppingcenter.com

# Payment Configuration
STRIPE_SECRET_KEY=sk_live_your_live_secret_key
STRIPE_PUBLIC_KEY=pk_live_your_live_public_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
STRIPE_SANDBOX=false

# Email Configuration
SMTP_HOST=smtp.your-email-provider.com
SMTP_PORT=587
SMTP_USERNAME=your_smtp_username
SMTP_PASSWORD=your_smtp_password
EMAIL_FROM=noreply@oceanshoppingcenter.com

# Monitoring Configuration
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=secure_grafana_password
```

#### Optional Configuration Variables
```bash
# Logistics (Shipping)
DHL_ENABLED=true
DHL_API_KEY=your_dhl_api_key
DHL_SANDBOX=false

FEDEX_ENABLED=true  
FEDEX_API_KEY=your_fedex_api_key
FEDEX_SANDBOX=false

# Performance Tuning
DB_POOL_SIZE=50
DB_POOL_MIN_IDLE=10
JAVA_OPTS=-XX:MaxRAMPercentage=75.0

# Rate Limiting
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX_REQUESTS=1000
```

### Configuration Files

#### PostgreSQL Configuration (`postgres/postgresql.conf`)
```conf
# Ocean Shopping Center - PostgreSQL Production Configuration

# Connection Settings
max_connections = 200
shared_buffers = 512MB
effective_cache_size = 1536MB
maintenance_work_mem = 128MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200

# Logging
log_destination = 'stderr'
logging_collector = on
log_directory = '/var/log/postgresql'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_min_duration_statement = 100
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Performance
work_mem = 4MB
huge_pages = try
max_worker_processes = 8
max_parallel_workers_per_gather = 2
max_parallel_workers = 8
```

#### Redis Configuration (`redis/redis-master.conf`)
```conf
# Ocean Shopping Center - Redis Master Configuration

# Basic Configuration  
bind 0.0.0.0
port 6379
protected-mode yes
requirepass secure_redis_password_2025

# Memory Management
maxmemory 1gb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# Persistence
save 900 1
save 300 10
save 60 10000
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb

# Logging
loglevel notice
logfile "/var/log/redis/redis.log"

# Performance
tcp-keepalive 300
timeout 0
tcp-backlog 511
```

## 🚦 Deployment Process

### Step-by-Step Production Deployment

#### 1. Pre-Deployment Preparation
```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Install Docker and Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo curl -L "https://github.com/docker/compose/releases/download/v2.21.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Create application directory
sudo mkdir -p /opt/ocean-shopping-center
cd /opt/ocean-shopping-center

# Clone repository
git clone https://github.com/your-org/ocean-shopping-center.git .
```

#### 2. Environment Configuration
```bash
# Copy and configure environment variables
cp .env.template .env.production

# Edit production environment variables
sudo nano .env.production

# Set secure file permissions
chmod 600 .env.production
```

#### 3. SSL Certificate Setup
```bash
# Create SSL directory
mkdir -p nginx/ssl

# Option A: Let's Encrypt (Certbot)
sudo apt install certbot
sudo certbot certonly --standalone -d oceanshoppingcenter.com -d api.oceanshoppingcenter.com

# Copy certificates
sudo cp /etc/letsencrypt/live/oceanshoppingcenter.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/oceanshoppingcenter.com/privkey.pem nginx/ssl/

# Option B: Manual certificate installation
# Copy your SSL certificates to nginx/ssl/
# - fullchain.pem (certificate + intermediate)
# - privkey.pem (private key)
```

#### 4. Database Setup
```bash
# Create database initialization scripts
mkdir -p postgres/init

# Create database backup directory
mkdir -p backups

# Set database permissions
chmod 700 postgres/init backups
```

#### 5. Application Deployment
```bash
# Pull latest images or build locally
docker-compose -f docker-compose.prod.yml pull

# Build custom images
docker-compose -f docker-compose.prod.yml build

# Start database and cache first
docker-compose -f docker-compose.prod.yml up -d postgres redis-master redis-replica

# Wait for database to be ready
sleep 30

# Run database migrations (if needed)
docker-compose -f docker-compose.prod.yml run --rm backend java -jar app.jar --spring.jpa.hibernate.ddl-auto=update

# Start application services
docker-compose -f docker-compose.prod.yml up -d backend frontend

# Start monitoring stack
docker-compose -f docker-compose.prod.yml up -d prometheus grafana elasticsearch kibana

# Start nginx proxy
docker-compose -f docker-compose.prod.yml up -d nginx-proxy

# Start backup service
docker-compose -f docker-compose.prod.yml up -d postgres-backup
```

#### 6. Post-Deployment Verification
```bash
# Check all services are running
docker-compose -f docker-compose.prod.yml ps

# Check application health
curl -f https://api.oceanshoppingcenter.com/actuator/health
curl -f https://oceanshoppingcenter.com/health

# Check logs for errors
docker-compose -f docker-compose.prod.yml logs --tail=100

# Run health checks
./scripts/health-check.sh
```

## 📊 Performance Optimization

### Database Performance Tuning

#### Index Creation
```bash
# Run index optimization
docker-compose -f docker-compose.prod.yml exec postgres psql -U ocean_user -d ocean_shopping_center -f /opt/indexes.sql
```

#### Connection Pool Optimization
```yaml
# Backend application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10  
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Application Performance

#### JVM Optimization
```bash
# Production JVM flags
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=32m \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"
```

#### Redis Optimization
```bash
# Redis performance tuning
echo 'vm.overcommit_memory = 1' | sudo tee -a /etc/sysctl.conf
echo 'net.core.somaxconn = 65535' | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

## 🔒 Security Configuration

### SSL/TLS Setup

#### Nginx SSL Configuration
```nginx
# nginx/ssl.conf
server {
    listen 443 ssl http2;
    server_name oceanshoppingcenter.com;
    
    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    # SSL Security
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
}
```

### Firewall Configuration
```bash
# UFW Firewall setup
sudo ufw --force reset
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (change port as needed)
sudo ufw allow 22/tcp

# Allow HTTP and HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow application ports (if needed)
sudo ufw allow 8080/tcp  # Backend (if direct access needed)
sudo ufw allow 5432/tcp  # PostgreSQL (if remote access needed)

# Enable firewall
sudo ufw --force enable
```

### Application Security
```yaml
# Spring Security Configuration
security:
  headers:
    frame-options: SAMEORIGIN
    content-type-options: nosniff
    xss-protection: 1; mode=block
    referrer-policy: strict-origin-when-cross-origin
    content-security-policy: "default-src 'self'; script-src 'self' 'unsafe-inline'"
```

## 📈 Monitoring and Observability

### Prometheus Metrics Collection
Access Prometheus at `http://your-server:9090`

Key metrics to monitor:
- `http_requests_total` - Request count by endpoint
- `http_request_duration_seconds` - Response time percentiles  
- `hikaricp_connections` - Database connection pool usage
- `jvm_memory_used_bytes` - JVM memory utilization
- `system_cpu_usage` - CPU utilization
- `redis_connected_clients` - Redis connection count

### Grafana Dashboards
Access Grafana at `http://your-server:3000`

Pre-configured dashboards:
- **Application Performance**: Response times, throughput, error rates
- **Database Monitoring**: Connection pool, query performance, cache hit rates
- **System Resources**: CPU, memory, disk usage, network I/O
- **Business Metrics**: User registrations, orders, revenue trends

### Log Management (ELK Stack)
Access Kibana at `http://your-server:5601`

Log sources:
- Application logs (Spring Boot structured logging)
- Nginx access and error logs  
- Database slow query logs
- System logs (syslog)

### Alerting Configuration
```yaml
# prometheus/alerts.yml
groups:
  - name: ocean-shopping-center
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: DatabaseConnectionsHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool usage high"
```

## 🧪 Load Testing

### Running Load Tests
```bash
# Install K6
curl https://github.com/grafana/k6/releases/download/v0.47.0/k6-v0.47.0-linux-amd64.tar.gz -L | tar xvz --strip-components 1

# Run load tests
./k6 run testing/load-tests/k6-load-test.js

# Run with custom configuration
./k6 run --vus 1000 --duration 10m testing/load-tests/k6-load-test.js

# Run with environment variables
BASE_URL=https://api.oceanshoppingcenter.com ./k6 run testing/load-tests/k6-load-test.js
```

### Performance Targets
- **Response Time**: P95 < 200ms, P99 < 500ms
- **Throughput**: 1000+ requests/second sustained
- **Concurrent Users**: 10,000+ simultaneous users
- **Error Rate**: < 0.1% under normal load
- **Database Response**: P95 < 50ms for queries

## 🔄 Backup and Recovery

### Database Backup Strategy
```bash
# Automated daily backups (configured in docker-compose.prod.yml)
# Backup retention: 30 days, 4 weeks, 6 months

# Manual backup
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U ocean_user ocean_shopping_center > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
docker-compose -f docker-compose.prod.yml exec -i postgres psql -U ocean_user ocean_shopping_center < backup_file.sql
```

### Application Data Backup
```bash
# Backup uploaded files
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz backend/uploads/

# Backup configuration
tar -czf config_backup_$(date +%Y%m%d).tar.gz .env.production nginx/ postgres/ redis/
```

### Disaster Recovery Plan
1. **Database Recovery**: Restore from latest automated backup
2. **Application Recovery**: Redeploy from Git repository
3. **File Recovery**: Restore uploads from backup
4. **Configuration Recovery**: Restore configuration files
5. **SSL Certificate Recovery**: Restore or regenerate certificates

## 🚀 Deployment Automation

### CI/CD Integration
```yaml
# .github/workflows/deploy.yml
name: Production Deployment
on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Production
        run: |
          ssh production-server 'cd /opt/ocean-shopping-center && git pull && docker-compose -f docker-compose.prod.yml up -d --build'
          
      - name: Health Check
        run: |
          curl -f https://api.oceanshoppingcenter.com/actuator/health
```

### Blue-Green Deployment
```bash
# Blue-Green deployment script
./scripts/blue-green-deploy.sh --env production --version v1.2.0
```

## 📋 Maintenance Tasks

### Daily Tasks
- [ ] Check application health endpoints
- [ ] Review error logs and metrics
- [ ] Monitor resource usage
- [ ] Verify backup completion

### Weekly Tasks  
- [ ] Review security logs
- [ ] Update dependency vulnerabilities
- [ ] Performance analysis and optimization
- [ ] Database maintenance (vacuum, analyze)

### Monthly Tasks
- [ ] Security assessment and penetration testing
- [ ] Capacity planning review
- [ ] Disaster recovery testing
- [ ] SSL certificate renewal check

## 🚨 Troubleshooting

### Common Issues and Solutions

#### Application Won't Start
```bash
# Check container logs
docker-compose -f docker-compose.prod.yml logs backend

# Check database connectivity
docker-compose -f docker-compose.prod.yml exec backend nc -z postgres 5432

# Check Redis connectivity  
docker-compose -f docker-compose.prod.yml exec backend nc -z redis-master 6379
```

#### High Response Times
```bash
# Check database performance
docker-compose -f docker-compose.prod.yml exec postgres psql -U ocean_user -d ocean_shopping_center -c "SELECT * FROM performance_summary;"

# Check connection pool
curl https://api.oceanshoppingcenter.com/actuator/metrics/hikaricp.connections.active
```

#### Memory Issues
```bash
# Check JVM memory usage
curl https://api.oceanshoppingcenter.com/actuator/metrics/jvm.memory.used

# Check container memory usage
docker stats
```

### Emergency Contacts and Procedures
1. **Database Issues**: Restore from backup, contact DBA
2. **Security Incidents**: Run security scan, check audit logs
3. **Performance Problems**: Scale horizontally, check resource usage
4. **SSL Certificate Issues**: Renew certificates, update nginx configuration

## 📞 Support and Maintenance

### Monitoring Endpoints
- **Application Health**: `https://api.oceanshoppingcenter.com/actuator/health`
- **Metrics**: `https://api.oceanshoppingcenter.com/actuator/metrics`  
- **Prometheus**: `http://your-server:9090`
- **Grafana**: `http://your-server:3000`
- **Kibana**: `http://your-server:5601`

### Log Locations
```bash
# Application logs
./backend/logs/
./nginx/logs/

# Database logs  
./postgres/logs/

# System logs
/var/log/syslog
journalctl -u docker
```

---

**🏁 Deployment Complete!** Your Ocean Shopping Center is now running in production with enterprise-grade performance, security, and monitoring capabilities.

For additional support, refer to the troubleshooting guide or contact the development team.