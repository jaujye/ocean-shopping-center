# Operational Runbook - Distributed Lock System

> **Document Type**: Operational Runbook  
> **System**: Redis Distributed Lock Framework  
> **Owner**: Ocean Shopping Center DevOps Team  
> **Last Updated**: 2025-09-05  

## 🚨 Emergency Procedures

### 1. Complete Lock System Failure

**Symptoms:**
- High error rates in application logs
- Cart operations failing
- Order processing blocked

**Immediate Actions:**
```bash
# 1. Check Redis cluster health
docker exec ocean-redis-master redis-cli ping
docker exec ocean-redis-slave-1 redis-cli ping

# 2. Check Sentinel status
docker exec ocean-redis-sentinel-1 redis-cli -p 26379 info sentinel

# 3. If Redis is down, restart cluster
cd /opt/ocean-shopping-center/docker/redis-ha
docker-compose -f docker-compose.redis-ha.yml restart

# 4. Force cleanup all locks (EMERGENCY ONLY)
curl -X POST -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/cleanup/force
```

**Recovery Time Objective (RTO):** < 5 minutes  
**Recovery Point Objective (RPO):** 0 (locks are ephemeral)

### 2. Lock Contention Storm

**Symptoms:**
- Lock acquisition times > 100ms
- Success rate < 80%
- Customer complaints about slow checkout

**Immediate Actions:**
```bash
# 1. Check lock statistics
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/statistics

# 2. Identify hot locks
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/status

# 3. Scale Redis if needed
docker-compose -f docker-compose.redis-ha.yml up -d --scale redis-slave=4

# 4. Temporarily increase lock TTL
kubectl patch configmap app-config -p '{"data":{"app.lock.default-ttl-seconds":"60"}}'
kubectl rollout restart deployment ocean-shopping-backend
```

### 3. Redis Sentinel Failover

**Symptoms:**
- Connection errors to Redis master
- Sentinel logs showing failover activity

**Actions:**
```bash
# 1. Monitor failover progress
docker logs -f ocean-redis-sentinel-1 | grep failover

# 2. Verify new master
docker exec ocean-redis-sentinel-1 redis-cli -p 26379 sentinel masters

# 3. Check application reconnection
curl https://api.ocean-shopping.com/api/admin/locks/health

# 4. Update monitoring dashboards with new topology
```

**Expected Failover Time:** < 30 seconds

## 📊 Monitoring & Alerting

### Critical Alerts

#### High Lock Failure Rate
```yaml
Alert: LockHighFailureRate
Condition: lock_failures_total / lock_acquisitions_total > 0.05
Threshold: > 5% failure rate for 2 minutes
Action: Page on-call engineer
```

#### Lock Acquisition Time High
```yaml
Alert: LockAcquisitionSlow
Condition: avg(lock_acquisition_time_seconds) > 0.01
Threshold: > 10ms average for 5 minutes
Action: Slack notification to dev team
```

#### Redis Connection Issues
```yaml
Alert: RedisConnectionFailure
Condition: redis_up == 0
Threshold: Redis instance down for 30 seconds
Action: Page on-call engineer
```

### Monitoring Dashboards

#### Grafana Dashboard Queries

**Lock Operations Rate:**
```promql
rate(lock_acquisitions_total[5m])
```

**Lock Success Rate:**
```promql
rate(lock_acquisitions_total[5m]) / (rate(lock_acquisitions_total[5m]) + rate(lock_failures_total[5m])) * 100
```

**Active Locks:**
```promql
lock_active_count
```

**Average Lock Hold Time:**
```promql
rate(lock_hold_duration_seconds_sum[5m]) / rate(lock_hold_duration_seconds_count[5m])
```

### Health Check Endpoints

| Endpoint | Purpose | Expected Response |
|----------|---------|-------------------|
| `/api/admin/locks/health` | Load balancer health check | HTTP 200, `{"status": "UP"}` |
| `/api/admin/locks/status` | System status | HTTP 200 with statistics |
| `/actuator/health` | Spring Boot health | HTTP 200, includes Redis status |

## 🔧 Maintenance Procedures

### Daily Tasks

#### Morning Health Check (5 minutes)
```bash
#!/bin/bash
# Daily lock system health check

echo "=== Daily Lock System Health Check ==="
echo "Date: $(date)"

# 1. Check Redis cluster health
echo "1. Redis Cluster Health:"
for node in redis-master redis-slave-1 redis-slave-2; do
    echo -n "$node: "
    docker exec ocean-$node redis-cli ping
done

# 2. Check lock statistics
echo "2. Lock Statistics:"
curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/statistics | jq .

# 3. Check for any orphaned locks
echo "3. Orphaned Lock Check:"
curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/cleanup-status | jq .

# 4. Performance metrics
echo "4. Performance Summary:"
curl -s http://localhost:9090/api/v1/query?query=avg_over_time\(lock_acquisition_time_seconds[24h]\) \
     | jq '.data.result[0].value[1]' | xargs printf "Average lock time (24h): %.3f seconds\n"

echo "=== Health check complete ==="
```

### Weekly Tasks

#### Performance Analysis (30 minutes)
```bash
#!/bin/bash
# Weekly performance analysis

echo "=== Weekly Lock Performance Analysis ==="

# 1. Top contended locks
echo "1. Most Contended Locks (last 7 days):"
curl -s "http://localhost:9090/api/v1/query?query=topk(10,sum by (lock_key) (increase(lock_failures_total[7d])))" \
     | jq -r '.data.result[] | "\(.metric.lock_key): \(.value[1]) failures"'

# 2. Lock hold time analysis
echo "2. Lock Hold Time Percentiles:"
for p in 50 95 99; do
    query="histogram_quantile(0.$p, sum(rate(lock_hold_duration_seconds_bucket[7d])) by (le))"
    curl -s "http://localhost:9090/api/v1/query?query=$query" \
         | jq -r ".data.result[0].value[1]" \
         | xargs printf "P$p: %.3f seconds\n"
done

# 3. Redis memory usage
echo "3. Redis Memory Usage:"
docker exec ocean-redis-master redis-cli info memory | grep used_memory_human

# 4. Generate weekly report
cat > weekly_lock_report.md << EOF
# Weekly Lock System Report - $(date +%Y-%m-%d)

## Summary
- Total lock operations: $(curl -s "http://localhost:9090/api/v1/query?query=increase(lock_acquisitions_total[7d])" | jq -r '.data.result[0].value[1]')
- Average success rate: $(curl -s "http://localhost:9090/api/v1/query?query=avg_over_time(lock_success_rate[7d])" | jq -r '.data.result[0].value[1]' | xargs printf "%.2f%%")
- P95 lock acquisition time: $(curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95, sum(rate(lock_acquisition_time_seconds_bucket[7d])) by (le))" | jq -r '.data.result[0].value[1]' | xargs printf "%.3f seconds")

## Recommendations
$(if (( $(curl -s "..." | jq -r '.data.result[0].value[1]' | cut -d. -f1) > 5 )); then echo "- Consider increasing lock timeout"; fi)
EOF

echo "Report saved to weekly_lock_report.md"
```

### Monthly Tasks

#### Capacity Planning (60 minutes)
```bash
#!/bin/bash
# Monthly capacity planning analysis

echo "=== Monthly Capacity Planning ==="

# 1. Growth analysis
echo "1. Lock Operation Growth:"
current_month=$(curl -s "http://localhost:9090/api/v1/query?query=increase(lock_acquisitions_total[30d])" | jq -r '.data.result[0].value[1]')
previous_month=$(curl -s "http://localhost:9090/api/v1/query?query=increase(lock_acquisitions_total[60d])-increase(lock_acquisitions_total[30d])" | jq -r '.data.result[0].value[1]')
growth_rate=$(echo "scale=2; ($current_month - $previous_month) / $previous_month * 100" | bc)
echo "Current month: $current_month operations"
echo "Previous month: $previous_month operations"
echo "Growth rate: $growth_rate%"

# 2. Redis resource usage trends
echo "2. Redis Resource Trends:"
docker exec ocean-redis-master redis-cli info memory | grep -E "(used_memory:|maxmemory:)"
docker exec ocean-redis-master redis-cli info stats | grep total_connections_received

# 3. Capacity recommendations
echo "3. Capacity Recommendations:"
if (( $(echo "$growth_rate > 20" | bc -l) )); then
    echo "- High growth detected. Consider scaling Redis cluster"
    echo "- Monitor lock contention patterns"
    echo "- Plan for additional Redis slaves"
fi

# 4. Performance benchmark
echo "4. Monthly Performance Benchmark:"
cd /opt/ocean-shopping-center
ENABLE_LOAD_TESTS=true mvn test -Dtest=DistributedLockLoadTest -q
```

## 🔍 Troubleshooting Guide

### Problem: Lock Acquisition Timeouts

**Diagnosis:**
```bash
# Check current lock statistics
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/statistics

# Examine Redis slow log
docker exec ocean-redis-master redis-cli slowlog get 10

# Check for specific lock contention
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     "https://api.ocean-shopping.com/api/admin/locks/check/cart:user:12345"
```

**Common Causes:**
1. High lock contention on popular resources
2. Long-running critical sections
3. Network latency to Redis
4. Redis memory pressure

**Solutions:**
1. Optimize critical section code
2. Implement lock-free alternatives where possible
3. Increase Redis memory allocation
4. Add Redis read replicas

### Problem: Orphaned Locks

**Diagnosis:**
```bash
# Check cleanup service status
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/cleanup-status

# Manual lock inspection
docker exec ocean-redis-master redis-cli keys "lock:*"
docker exec ocean-redis-master redis-cli ttl "lock:cart:user:12345"
```

**Solutions:**
```bash
# Trigger manual cleanup
curl -X POST -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/cleanup/force

# Adjust cleanup frequency
kubectl patch configmap app-config -p '{
  "data": {
    "app.lock.cleanup.orphaned-threshold-minutes": "3"
  }
}'
```

### Problem: Redis Failover Issues

**Diagnosis:**
```bash
# Check Sentinel status
for i in 1 2 3; do
    echo "Sentinel $i:"
    docker exec ocean-redis-sentinel-$i redis-cli -p 26379 info sentinel
done

# Check master status
docker exec ocean-redis-sentinel-1 redis-cli -p 26379 sentinel masters

# Application connectivity
curl https://api.ocean-shopping.com/api/admin/locks/health
```

**Solutions:**
```bash
# Reset Sentinel if needed
docker-compose -f docker-compose.redis-ha.yml restart redis-sentinel-1

# Manual failover (if required)
docker exec ocean-redis-sentinel-1 redis-cli -p 26379 \
  sentinel failover ocean-master
```

## 📋 Configuration Management

### Environment Variables

| Variable | Development | Production | Description |
|----------|-------------|------------|-------------|
| `REDIS_HOST` | localhost | sentinel-lb | Redis host |
| `REDIS_PORT` | 6379 | 6379 | Redis port |
| `REDIS_PASSWORD` | - | ${SECRET} | Redis auth |
| `REDIS_SSL_ENABLED` | false | true | Enable SSL |
| `REDIS_SENTINEL_MASTER` | - | ocean-master | Master name |
| `REDIS_SENTINEL_NODES` | - | sentinel1:26379,... | Sentinel nodes |

### Configuration Templates

**Development:**
```yaml
app:
  lock:
    enabled: true
    default-ttl-seconds: 10
    max-retries: 3
    cleanup:
      enabled: true

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**Production:**
```yaml
app:
  lock:
    enabled: true
    default-ttl-seconds: 30
    max-retries: 5
    cleanup:
      enabled: true
      orphaned-threshold-minutes: 5

spring:
  data:
    redis:
      sentinel:
        master: ocean-master
        nodes: sentinel1:26379,sentinel2:26379,sentinel3:26379
      ssl:
        enabled: true
      password: ${REDIS_PASSWORD}
```

## 🚀 Deployment Procedures

### Rolling Update Process

```bash
#!/bin/bash
# Safe deployment with distributed locks

echo "Starting rolling update with lock system..."

# 1. Verify current system health
curl -f https://api.ocean-shopping.com/api/admin/locks/health || {
    echo "Lock system unhealthy, aborting deployment"
    exit 1
}

# 2. Update application configuration
kubectl apply -f k8s/configmap-locks.yaml

# 3. Rolling restart with proper sequencing
kubectl rollout restart deployment ocean-shopping-backend
kubectl rollout status deployment ocean-shopping-backend --timeout=300s

# 4. Verify new deployment
sleep 30
curl -f https://api.ocean-shopping.com/api/admin/locks/health || {
    echo "New deployment unhealthy, consider rollback"
    kubectl rollout undo deployment ocean-shopping-backend
    exit 1
}

# 5. Run smoke tests
ENABLE_LOAD_TESTS=false mvn test -Dtest=DistributedLockIntegrationTest

echo "Deployment successful"
```

### Rollback Procedure

```bash
#!/bin/bash
# Emergency rollback procedure

echo "Emergency rollback of lock system changes..."

# 1. Rollback application
kubectl rollout undo deployment ocean-shopping-backend

# 2. Restore previous configuration
kubectl apply -f k8s/configmap-locks-previous.yaml

# 3. Wait for rollback completion
kubectl rollout status deployment ocean-shopping-backend --timeout=300s

# 4. Force cleanup any problematic locks
curl -X POST -H "Authorization: Bearer ${ADMIN_TOKEN}" \
     https://api.ocean-shopping.com/api/admin/locks/cleanup/force

# 5. Verify system health
curl -f https://api.ocean-shopping.com/api/admin/locks/health

echo "Rollback completed"
```

## 📞 Contact Information

### Escalation Matrix

| Severity | Response Time | Contact |
|----------|---------------|---------|
| P0 - System Down | 15 minutes | On-call engineer |
| P1 - Degraded Performance | 30 minutes | Dev team lead |
| P2 - Minor Issues | 4 hours | Development team |
| P3 - Questions | Next business day | Documentation |

### Team Contacts

- **Primary On-Call**: DevOps team rotation
- **Secondary**: Backend development team
- **Escalation**: Engineering manager

---

**Document Version**: 1.0  
**Next Review**: Monthly  
**Owner**: Ocean Shopping Center DevOps Team