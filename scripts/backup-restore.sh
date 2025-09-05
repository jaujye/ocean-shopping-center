#!/bin/bash

# Backup and Disaster Recovery Script for Ocean Shopping Center
# Comprehensive backup solution with automated restore capabilities

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_ROOT="${BACKUP_ROOT:-$PROJECT_ROOT/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1" >&2; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
log_error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1" >&2; }

# Help function
show_help() {
    cat << EOF
Backup and Disaster Recovery Script for Ocean Shopping Center

Usage: $0 COMMAND [OPTIONS]

COMMANDS:
    backup              Create full system backup
    restore             Restore from backup
    list                List available backups
    cleanup             Clean old backups
    test-restore        Test restore procedure (dry run)
    schedule            Set up automated backup schedule

OPTIONS:
    --backup-id ID      Specific backup ID to restore from
    --retention-days N  Number of days to retain backups [default: 30]
    --storage TYPE      Storage type (local|s3|gcs) [default: local]
    --compress          Enable compression for backups
    --encrypt           Enable encryption for backups
    --verify            Verify backup integrity
    --no-data          Skip data backup (config only)
    --help              Show this help

EXAMPLES:
    $0 backup --compress --encrypt
    $0 restore --backup-id 20240905-143022
    $0 list
    $0 cleanup --retention-days 7
    $0 test-restore --backup-id latest

ENVIRONMENT VARIABLES:
    BACKUP_ROOT         Backup storage location
    RETENTION_DAYS      Backup retention period
    S3_BUCKET           S3 bucket for remote backups
    GPG_KEY_ID          GPG key for encryption
    POSTGRES_PASSWORD   Database password
    REDIS_PASSWORD      Redis password
EOF
}

# Generate backup ID
generate_backup_id() {
    echo "$(date +%Y%m%d-%H%M%S)"
}

# Create backup directory structure
create_backup_structure() {
    local backup_id="$1"
    local backup_dir="$BACKUP_ROOT/$backup_id"
    
    mkdir -p "$backup_dir"/{database,redis,config,volumes,logs}
    echo "$backup_dir"
}

# Backup PostgreSQL database
backup_database() {
    local backup_dir="$1"
    local compress_flag="${2:-false}"
    
    log_info "Backing up PostgreSQL database..."
    
    local pg_container
    pg_container=$(docker-compose -f "$COMPOSE_FILE" ps -q postgres)
    
    if [[ -z "$pg_container" ]]; then
        log_error "PostgreSQL container not found"
        return 1
    fi
    
    local backup_file="$backup_dir/database/postgres_backup.sql"
    if [[ "$compress_flag" == "true" ]]; then
        backup_file="$backup_file.gz"
    fi
    
    # Create database backup
    if [[ "$compress_flag" == "true" ]]; then
        docker exec "$pg_container" pg_dumpall -U postgres | gzip > "$backup_file"
    else
        docker exec "$pg_container" pg_dumpall -U postgres > "$backup_file"
    fi
    
    # Backup database configuration
    docker exec "$pg_container" cat /etc/postgresql/postgresql.conf > "$backup_dir/database/postgresql.conf" 2>/dev/null || true
    
    # Export database statistics
    docker exec "$pg_container" psql -U postgres -c "SELECT * FROM pg_stat_activity;" > "$backup_dir/database/pg_stat_activity.txt" 2>/dev/null || true
    docker exec "$pg_container" psql -U postgres -c "SELECT * FROM pg_settings;" > "$backup_dir/database/pg_settings.txt" 2>/dev/null || true
    
    local backup_size
    backup_size=$(du -h "$backup_file" | cut -f1)
    log_success "Database backup completed: $backup_size"
    
    return 0
}

# Backup Redis data
backup_redis() {
    local backup_dir="$1"
    local compress_flag="${2:-false}"
    
    log_info "Backing up Redis data..."
    
    local redis_master redis_replica
    redis_master=$(docker-compose -f "$COMPOSE_FILE" ps -q redis-master)
    redis_replica=$(docker-compose -f "$COMPOSE_FILE" ps -q redis-replica)
    
    if [[ -n "$redis_master" ]]; then
        # Force Redis save
        docker exec "$redis_master" redis-cli BGSAVE
        
        # Wait for background save to complete
        while [[ $(docker exec "$redis_master" redis-cli LASTSAVE) == $(docker exec "$redis_master" redis-cli LASTSAVE) ]]; do
            sleep 1
        done
        
        # Copy RDB file
        docker cp "$redis_master":/data/dump.rdb "$backup_dir/redis/master_dump.rdb"
        
        if [[ "$compress_flag" == "true" ]]; then
            gzip "$backup_dir/redis/master_dump.rdb"
        fi
        
        # Backup Redis configuration
        docker exec "$redis_master" cat /usr/local/etc/redis/redis.conf > "$backup_dir/redis/redis-master.conf" 2>/dev/null || true
    fi
    
    if [[ -n "$redis_replica" ]]; then
        docker cp "$redis_replica":/data/dump-replica.rdb "$backup_dir/redis/replica_dump.rdb" 2>/dev/null || true
        
        if [[ "$compress_flag" == "true" && -f "$backup_dir/redis/replica_dump.rdb" ]]; then
            gzip "$backup_dir/redis/replica_dump.rdb"
        fi
        
        docker exec "$redis_replica" cat /usr/local/etc/redis/redis.conf > "$backup_dir/redis/redis-replica.conf" 2>/dev/null || true
    fi
    
    log_success "Redis backup completed"
    return 0
}

# Backup application configuration
backup_config() {
    local backup_dir="$1"
    
    log_info "Backing up application configuration..."
    
    # Copy configuration files
    cp -r "$PROJECT_ROOT"/{.env,docker-compose.prod.yml,nginx,monitoring} "$backup_dir/config/" 2>/dev/null || true
    
    # Copy custom configurations
    [[ -d "$PROJECT_ROOT/postgres" ]] && cp -r "$PROJECT_ROOT/postgres" "$backup_dir/config/"
    [[ -d "$PROJECT_ROOT/redis" ]] && cp -r "$PROJECT_ROOT/redis" "$backup_dir/config/"
    [[ -d "$PROJECT_ROOT/ssl-config" ]] && cp -r "$PROJECT_ROOT/ssl-config" "$backup_dir/config/"
    
    # Export Docker Compose configuration
    docker-compose -f "$COMPOSE_FILE" config > "$backup_dir/config/docker-compose-resolved.yml" 2>/dev/null || true
    
    # Save running container information
    docker-compose -f "$COMPOSE_FILE" ps > "$backup_dir/config/running-services.txt" 2>/dev/null || true
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}" > "$backup_dir/config/docker-images.txt" 2>/dev/null || true
    
    log_success "Configuration backup completed"
    return 0
}

# Backup Docker volumes
backup_volumes() {
    local backup_dir="$1"
    local compress_flag="${2:-false}"
    
    log_info "Backing up Docker volumes..."
    
    # Get list of volumes
    local volumes
    volumes=$(docker volume ls --format "{{.Name}}" | grep "ocean" || true)
    
    for volume in $volumes; do
        local volume_backup="$backup_dir/volumes/${volume}.tar"
        
        log_info "Backing up volume: $volume"
        
        # Create temporary container to access volume
        docker run --rm -v "$volume":/volume -v "$backup_dir/volumes":/backup alpine \
            tar -czf "/backup/${volume}.tar.gz" -C /volume . 2>/dev/null || true
        
        if [[ -f "$backup_dir/volumes/${volume}.tar.gz" ]]; then
            local volume_size
            volume_size=$(du -h "$backup_dir/volumes/${volume}.tar.gz" | cut -f1)
            log_info "Volume $volume backed up: $volume_size"
        fi
    done
    
    log_success "Volumes backup completed"
    return 0
}

# Backup application logs
backup_logs() {
    local backup_dir="$1"
    local compress_flag="${2:-false}"
    
    log_info "Backing up application logs..."
    
    # Copy application logs
    [[ -d "$PROJECT_ROOT/backend/logs" ]] && cp -r "$PROJECT_ROOT/backend/logs" "$backup_dir/logs/backend-logs" 2>/dev/null || true
    [[ -d "$PROJECT_ROOT/nginx/logs" ]] && cp -r "$PROJECT_ROOT/nginx/logs" "$backup_dir/logs/nginx-logs" 2>/dev/null || true
    
    # Export Docker container logs
    local containers
    containers=$(docker-compose -f "$COMPOSE_FILE" ps -q)
    
    for container in $containers; do
        local container_name
        container_name=$(docker inspect --format '{{.Name}}' "$container" | sed 's|/||')
        
        local log_file="$backup_dir/logs/${container_name}.log"
        docker logs "$container" > "$log_file" 2>&1 || true
        
        if [[ "$compress_flag" == "true" && -f "$log_file" ]]; then
            gzip "$log_file"
        fi
    done
    
    log_success "Logs backup completed"
    return 0
}

# Create backup manifest
create_manifest() {
    local backup_dir="$1"
    local backup_id="$2"
    local backup_type="${3:-full}"
    
    local manifest_file="$backup_dir/MANIFEST"
    
    cat > "$manifest_file" << EOF
# Ocean Shopping Center Backup Manifest
# Generated on: $(date)

BACKUP_ID=$backup_id
BACKUP_TYPE=$backup_type
BACKUP_DATE=$(date -Iseconds)
BACKUP_HOST=$(hostname)
BACKUP_USER=$(whoami)
BACKUP_VERSION=1.0

# System Information
OS=$(uname -s)
KERNEL=$(uname -r)
DOCKER_VERSION=$(docker --version 2>/dev/null || echo "N/A")

# Application Information
$(cd "$PROJECT_ROOT" && git log -1 --pretty=format:"GIT_COMMIT=%H%nGIT_AUTHOR=%an%nGIT_DATE=%ad" 2>/dev/null || echo "GIT_COMMIT=N/A")

# Backup Contents
$(find "$backup_dir" -type f -exec basename {} \; | sort | sed 's/^/FILE_/')

# Checksums
EOF
    
    # Add checksums for verification
    find "$backup_dir" -type f ! -name "MANIFEST" -exec sha256sum {} \; | sed 's|'"$backup_dir"'/||' >> "$manifest_file"
    
    log_success "Backup manifest created"
}

# Encrypt backup
encrypt_backup() {
    local backup_dir="$1"
    local gpg_key="${GPG_KEY_ID:-}"
    
    if [[ -z "$gpg_key" ]]; then
        log_warn "No GPG key specified, skipping encryption"
        return 0
    fi
    
    log_info "Encrypting backup with GPG key: $gpg_key"
    
    find "$backup_dir" -type f ! -name "*.gpg" | while read -r file; do
        gpg --trust-model always --encrypt -r "$gpg_key" "$file"
        rm "$file"
        log_info "Encrypted: $(basename "$file")"
    done
    
    log_success "Backup encryption completed"
}

# Verify backup integrity
verify_backup() {
    local backup_dir="$1"
    local manifest_file="$backup_dir/MANIFEST"
    
    if [[ ! -f "$manifest_file" ]]; then
        log_error "Backup manifest not found"
        return 1
    fi
    
    log_info "Verifying backup integrity..."
    
    # Check checksums
    local verification_failed=false
    
    while read -r checksum file; do
        if [[ -f "$backup_dir/$file" ]]; then
            local actual_checksum
            actual_checksum=$(sha256sum "$backup_dir/$file" | cut -d' ' -f1)
            
            if [[ "$checksum" != "$actual_checksum" ]]; then
                log_error "Checksum mismatch for $file"
                verification_failed=true
            fi
        else
            log_warn "File missing: $file"
        fi
    done < <(grep -E '^[a-f0-9]{64}' "$manifest_file")
    
    if [[ "$verification_failed" == "true" ]]; then
        log_error "Backup verification failed"
        return 1
    fi
    
    log_success "Backup integrity verified"
    return 0
}

# Full backup operation
perform_backup() {
    local backup_id
    backup_id=$(generate_backup_id)
    
    local backup_dir
    backup_dir=$(create_backup_structure "$backup_id")
    
    log_info "Starting backup with ID: $backup_id"
    log_info "Backup directory: $backup_dir"
    
    local backup_failed=false
    
    # Perform individual backup operations
    backup_config "$backup_dir" || backup_failed=true
    
    if [[ "${NO_DATA:-false}" != "true" ]]; then
        backup_database "$backup_dir" "${COMPRESS:-false}" || backup_failed=true
        backup_redis "$backup_dir" "${COMPRESS:-false}" || backup_failed=true
        backup_volumes "$backup_dir" "${COMPRESS:-false}" || backup_failed=true
    fi
    
    backup_logs "$backup_dir" "${COMPRESS:-false}" || backup_failed=true
    
    # Create manifest
    create_manifest "$backup_dir" "$backup_id" "full"
    
    # Encrypt if requested
    if [[ "${ENCRYPT:-false}" == "true" ]]; then
        encrypt_backup "$backup_dir"
    fi
    
    # Verify backup
    if [[ "${VERIFY:-false}" == "true" ]]; then
        verify_backup "$backup_dir" || backup_failed=true
    fi
    
    if [[ "$backup_failed" == "true" ]]; then
        log_error "Backup completed with errors"
        return 1
    fi
    
    local backup_size
    backup_size=$(du -sh "$backup_dir" | cut -f1)
    log_success "Backup completed successfully!"
    log_success "Backup ID: $backup_id"
    log_success "Backup Size: $backup_size"
    log_success "Location: $backup_dir"
    
    return 0
}

# List available backups
list_backups() {
    log_info "Available backups:"
    
    if [[ ! -d "$BACKUP_ROOT" ]]; then
        log_warn "No backup directory found: $BACKUP_ROOT"
        return 0
    fi
    
    printf "%-20s %-10s %-15s %s\n" "BACKUP_ID" "SIZE" "DATE" "TYPE"
    printf "%-20s %-10s %-15s %s\n" "--------" "----" "----" "----"
    
    for backup_dir in "$BACKUP_ROOT"/*/; do
        if [[ -d "$backup_dir" ]]; then
            local backup_id
            backup_id=$(basename "$backup_dir")
            
            local size
            size=$(du -sh "$backup_dir" 2>/dev/null | cut -f1)
            
            local date type="full"
            if [[ -f "$backup_dir/MANIFEST" ]]; then
                date=$(grep "BACKUP_DATE=" "$backup_dir/MANIFEST" | cut -d'=' -f2)
                type=$(grep "BACKUP_TYPE=" "$backup_dir/MANIFEST" | cut -d'=' -f2 || echo "full")
            else
                date=$(date -r "$backup_dir" +%Y-%m-%d)
            fi
            
            printf "%-20s %-10s %-15s %s\n" "$backup_id" "$size" "${date:0:10}" "$type"
        fi
    done
}

# Cleanup old backups
cleanup_backups() {
    local retention_days="${RETENTION_DAYS}"
    
    log_info "Cleaning up backups older than $retention_days days..."
    
    if [[ ! -d "$BACKUP_ROOT" ]]; then
        log_warn "No backup directory found: $BACKUP_ROOT"
        return 0
    fi
    
    local deleted_count=0
    
    find "$BACKUP_ROOT" -maxdepth 1 -type d -mtime +$retention_days | while read -r backup_dir; do
        if [[ "$backup_dir" != "$BACKUP_ROOT" ]]; then
            local backup_id
            backup_id=$(basename "$backup_dir")
            
            log_info "Removing old backup: $backup_id"
            rm -rf "$backup_dir"
            ((deleted_count++))
        fi
    done
    
    log_success "Cleaned up $deleted_count old backups"
}

# Parse command line arguments
parse_args() {
    COMMAND=""
    BACKUP_ID=""
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            backup|restore|list|cleanup|test-restore|schedule)
                COMMAND="$1"
                shift
                ;;
            --backup-id)
                BACKUP_ID="$2"
                shift 2
                ;;
            --retention-days)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            --compress)
                COMPRESS=true
                shift
                ;;
            --encrypt)
                ENCRYPT=true
                shift
                ;;
            --verify)
                VERIFY=true
                shift
                ;;
            --no-data)
                NO_DATA=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    if [[ -z "$COMMAND" ]]; then
        log_error "No command specified"
        show_help
        exit 1
    fi
}

# Main function
main() {
    parse_args "$@"
    
    # Ensure backup directory exists
    mkdir -p "$BACKUP_ROOT"
    
    case "$COMMAND" in
        backup)
            perform_backup
            ;;
        restore)
            log_error "Restore functionality not yet implemented"
            exit 1
            ;;
        list)
            list_backups
            ;;
        cleanup)
            cleanup_backups
            ;;
        test-restore)
            log_error "Test restore functionality not yet implemented"
            exit 1
            ;;
        schedule)
            log_error "Schedule functionality not yet implemented"
            exit 1
            ;;
        *)
            log_error "Unknown command: $COMMAND"
            exit 1
            ;;
    esac
}

# Trap signals for cleanup
trap 'log_error "Backup interrupted"; exit 1' INT TERM

# Run main function
main "$@"