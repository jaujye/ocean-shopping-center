# Ocean Shopping Center - Infrastructure Setup Guide

## Overview

This document provides comprehensive instructions for setting up the infrastructure for Ocean Shopping Center, including PostgreSQL database, Redis cache, Docker environment, and MCP tools integration.

## Prerequisites

- Docker and Docker Compose installed
- Git installed
- MCP tools configured (Context7, PostgreSQL MCP, Redis MCP, Graphiti)

## Quick Start

1. **Clone and navigate to the project:**
   ```bash
   git clone <repository-url>
   cd ocean-shopping-center
   git checkout epic/ocean-shopping-center
   ```

2. **Set up environment variables:**
   ```bash
   cp .env.template .env
   # Edit .env with your specific configuration
   ```

3. **Start the infrastructure:**
   ```bash
   docker compose up -d postgres redis
   ```

4. **Initialize the database:**
   ```bash
   # Database schema is automatically created via MCP tools
   # Or manually execute: docker compose exec postgres psql -U postgres -d ocean_shopping_center -f /docker-entrypoint-initdb.d/ocean_shopping_center.sql
   ```

## Architecture Overview

### Database Schema (PostgreSQL)

The database consists of 18 core tables organized into functional areas:

#### User Management
- **users**: User accounts with role-based access (customer, store_owner, administrator)
- **user_addresses**: Multiple shipping/billing addresses per user

#### Multi-Tenant Store Management
- **stores**: Store tenants with individual configuration
- **categories**: Hierarchical product categories

#### Product Catalog
- **products**: Main product catalog with search optimization
- **product_variants**: Product variations (size, color, etc.)
- **product_images**: Product image gallery

#### Shopping & Orders
- **carts**: Shopping cart sessions
- **cart_items**: Cart contents
- **orders**: Comprehensive order tracking
- **order_items**: Order line items
- **payments**: Payment processing records

#### Shipping & Logistics
- **shipping_carriers**: Shipping companies (UPS, FedEx, USPS, DHL)
- **shipping_methods**: Shipping service levels
- **shipments**: Package tracking
- **shipment_events**: Tracking history

#### Communication
- **conversations**: Customer support tickets
- **messages**: Chat messages and support communication

#### System
- **system_settings**: Application configuration
- **audit_logs**: System audit trail (when implemented)

### Redis Configuration

Redis is configured with multiple databases for different purposes:

- **Database 0**: Session storage (24-hour TTL)
- **Database 1**: Product cache (10-minute TTL)
- **Database 2**: User profile cache (15-minute TTL)
- **Database 3**: Shopping cart cache
- **Database 4**: General application cache
- **Databases 5-15**: Reserved for future use

### Key Features

#### Search Optimization
- Full-text search using PostgreSQL `tsvector`
- Trigram indexes for fuzzy matching
- Optimized product name and description searches

#### Performance Optimization
- Strategic indexing for high-concurrency operations
- Proper foreign key relationships with cascade rules
- Automatic `updated_at` timestamp triggers
- Connection pooling ready

#### Multi-Tenancy
- Store-based data isolation
- Commission rate tracking per store
- Individual store settings and branding

## Environment Configuration

### Development Environment (.env)

```bash
# Database
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/ocean_shopping_center
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=ocean_shopping_center
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis
REDIS_URL=redis://localhost:6379
REDIS_HOST=localhost
REDIS_PORT=6379

# Application
NODE_ENV=development
BACKEND_PORT=3000
FRONTEND_PORT=3001

# Development Tools
PGADMIN_PORT=8080
REDIS_COMMANDER_PORT=8081
```

### Production Environment

For production, ensure you:

1. **Use strong passwords:**
   ```bash
   POSTGRES_PASSWORD=<strong-secure-password>
   REDIS_PASSWORD=<redis-password>
   ```

2. **Configure SSL/TLS:**
   ```bash
   DATABASE_URL=postgresql://user:pass@host:5432/db?sslmode=require
   ```

3. **Set up proper networking and firewalls**

## Docker Services

### Core Services

- **postgres**: PostgreSQL 15 with alpine base
- **redis**: Redis 7 with persistence configuration
- **pgadmin**: Database management interface (development profile)
- **redis-commander**: Redis management interface (development profile)

### Service Health Checks

All services include health checks:
- PostgreSQL: `pg_isready` command
- Redis: `redis-cli ping` command

### Volume Management

Persistent volumes for data:
- `postgres_data`: Database files
- `redis_data`: Redis persistence
- `pgadmin_data`: pgAdmin configuration

## MCP Tools Integration

### PostgreSQL MCP

The PostgreSQL MCP tool provides:
- Schema introspection
- Query execution and analysis
- Performance optimization recommendations
- Database health monitoring

### Redis MCP

The Redis MCP tool provides:
- Key-value operations
- Cache management
- Session storage operations

### Graphiti Memory

The Graphiti MCP tool provides:
- Customer interaction memory
- Support conversation tracking
- Relationship mapping between entities

## Database Sample Data

The system includes sample data for testing:

### Users
- Admin user: `admin@oceanshoppingcenter.com` (administrator)
- Customer: `john.doe@example.com` (customer)
- Store owner: `jane.smith@example.com` (store_owner)

### Sample Store
- **Tech Paradise**: Electronics store with sample products

### Sample Products
- MacBook Pro 14" ($2,499.00)
- iPhone 15 Pro ($999.00)

## Testing the Setup

### Database Connection Test

```sql
-- Verify tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Test sample data
SELECT u.email, u.role, s.name as store_name 
FROM users u 
LEFT JOIN stores s ON u.id = s.owner_id;
```

### Redis Cache Test

```bash
# Test basic operations
redis-cli ping
redis-cli set test_key "test_value"
redis-cli get test_key
```

### MCP Tools Test

The infrastructure includes MCP tool integration tests that verify:
- PostgreSQL connection and schema access
- Redis caching operations
- Sample data insertion and retrieval

## Development Tools

### pgAdmin (Database Management)
- **URL**: http://localhost:8080
- **Email**: admin@oceanshoppingcenter.com
- **Password**: admin

### Redis Commander (Cache Management)
- **URL**: http://localhost:8081
- **Username**: admin
- **Password**: admin

## Security Considerations

### Database Security
- Use strong passwords in production
- Enable SSL connections
- Implement proper user roles and permissions
- Regular security updates

### Redis Security
- Configure password authentication
- Use SSL/TLS in production
- Implement proper network security
- Regular security updates

### Application Security
- JWT tokens for authentication
- Bcrypt for password hashing
- Session timeout configuration
- CORS configuration

## Monitoring and Maintenance

### Database Monitoring
- Monitor connection pools
- Track query performance
- Regular backup schedules
- Index optimization

### Cache Monitoring
- Monitor memory usage
- Track hit/miss ratios
- Monitor expired key cleanup
- Connection pool monitoring

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check if PostgreSQL container is running
   - Verify environment variables
   - Check network connectivity

2. **Redis Connection Failed**
   - Verify Redis container status
   - Check Redis configuration
   - Verify port accessibility

3. **Schema Creation Failed**
   - Check PostgreSQL logs
   - Verify user permissions
   - Check for syntax errors

### Logs

Access container logs:
```bash
docker compose logs postgres
docker compose logs redis
docker compose logs pgadmin
```

## Performance Optimization

### Database Optimization
- Regular `VACUUM` and `ANALYZE`
- Index monitoring and optimization
- Query performance analysis
- Connection pool tuning

### Cache Optimization
- Memory usage monitoring
- TTL optimization
- Key pattern optimization
- Connection pool tuning

## Backup and Recovery

### Database Backups
```bash
# Create backup
docker compose exec postgres pg_dump -U postgres ocean_shopping_center > backup.sql

# Restore backup
docker compose exec -T postgres psql -U postgres ocean_shopping_center < backup.sql
```

### Redis Persistence
Redis is configured with both RDB and AOF persistence:
- RDB snapshots for point-in-time recovery
- AOF for write operation logging

## Next Steps

After completing the infrastructure setup:

1. **Backend Development**: Implement REST API services
2. **Frontend Development**: Create user interfaces
3. **Authentication**: Implement JWT-based authentication
4. **Payment Integration**: Add payment processing
5. **Testing**: Comprehensive testing suite
6. **Deployment**: Production deployment configuration

## Support

For issues and questions:
- Check the troubleshooting section
- Review container logs
- Consult MCP tool documentation
- Review PostgreSQL and Redis documentation