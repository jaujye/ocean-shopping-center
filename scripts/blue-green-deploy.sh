#!/bin/bash

# Blue-Green Deployment Script for Ocean Shopping Center
# This script implements zero-downtime deployment with automatic rollback

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"
HEALTH_CHECK_TIMEOUT=300
HEALTH_CHECK_INTERVAL=10
ROLLBACK_ON_FAILURE=true

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" >&2
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" >&2
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" >&2
}

# Help function
show_help() {
    cat << EOF
Blue-Green Deployment Script for Ocean Shopping Center

Usage: $0 [OPTIONS]

OPTIONS:
    -e, --environment ENV       Target environment (staging|production) [default: staging]
    -v, --version VERSION       Application version to deploy [default: latest]
    -t, --timeout SECONDS      Health check timeout [default: 300]
    -r, --no-rollback          Disable automatic rollback on failure
    -d, --dry-run              Show what would be deployed without executing
    -h, --help                 Show this help message

EXAMPLES:
    $0 --environment production --version v1.2.3
    $0 --dry-run --version latest
    $0 --no-rollback --environment staging

ENVIRONMENT VARIABLES:
    DOCKER_REGISTRY            Docker registry URL
    APP_VERSION               Application version (overrides --version)
    DEPLOY_ENVIRONMENT        Target environment (overrides --environment)
    HEALTH_CHECK_URL          Health check endpoint URL
    SLACK_WEBHOOK_URL         Slack webhook for notifications
EOF
}

# Parse command line arguments
parse_args() {
    ENVIRONMENT="${DEPLOY_ENVIRONMENT:-staging}"
    VERSION="${APP_VERSION:-latest}"
    DRY_RUN=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -v|--version)
                VERSION="$2"
                shift 2
                ;;
            -t|--timeout)
                HEALTH_CHECK_TIMEOUT="$2"
                shift 2
                ;;
            -r|--no-rollback)
                ROLLBACK_ON_FAILURE=false
                shift
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -h|--help)
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
    
    # Validate environment
    if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
        log_error "Invalid environment: $ENVIRONMENT. Must be 'staging' or 'production'"
        exit 1
    fi
    
    # Set environment-specific variables
    if [[ "$ENVIRONMENT" == "production" ]]; then
        ENV_FILE="$PROJECT_ROOT/.env.production"
        COMPOSE_PROJECT_NAME="ocean_prod"
        HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-https://api.oceanshoppingcenter.com/health}"
    else
        ENV_FILE="$PROJECT_ROOT/.env.staging"
        COMPOSE_PROJECT_NAME="ocean_staging"
        HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-http://staging-api.oceanshoppingcenter.com/health}"
    fi
    
    # Load environment variables
    if [[ -f "$ENV_FILE" ]]; then
        source "$ENV_FILE"
    else
        log_warn "Environment file not found: $ENV_FILE"
    fi
}

# Send notification to Slack
send_notification() {
    local message="$1"
    local status="${2:-info}"  # info, success, warning, error
    
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        local color
        case "$status" in
            success) color="#36a64f" ;;
            warning) color="#ff9500" ;;
            error) color="#ff0000" ;;
            *) color="#36a64f" ;;
        esac
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"attachments\":[{\"color\":\"$color\",\"text\":\"$message\"}]}" \
            "$SLACK_WEBHOOK_URL" 2>/dev/null || true
    fi
}

# Check if service is healthy
check_service_health() {
    local service_name="$1"
    local max_attempts=$((HEALTH_CHECK_TIMEOUT / HEALTH_CHECK_INTERVAL))
    local attempt=1
    
    log_info "Checking health of $service_name..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" \
           exec -T "$service_name" curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
            log_success "$service_name is healthy"
            return 0
        fi
        
        log_warn "Health check attempt $attempt/$max_attempts failed for $service_name"
        sleep $HEALTH_CHECK_INTERVAL
        ((attempt++))
    done
    
    log_error "$service_name failed health check after $HEALTH_CHECK_TIMEOUT seconds"
    return 1
}

# Check external health endpoint
check_external_health() {
    local max_attempts=$((HEALTH_CHECK_TIMEOUT / HEALTH_CHECK_INTERVAL))
    local attempt=1
    
    log_info "Checking external health endpoint: $HEALTH_CHECK_URL"
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s -m 10 "$HEALTH_CHECK_URL" >/dev/null 2>&1; then
            log_success "External health check passed"
            return 0
        fi
        
        log_warn "External health check attempt $attempt/$max_attempts failed"
        sleep $HEALTH_CHECK_INTERVAL
        ((attempt++))
    done
    
    log_error "External health check failed after $HEALTH_CHECK_TIMEOUT seconds"
    return 1
}

# Get current running services
get_running_services() {
    docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" ps -q --services --filter "status=running" 2>/dev/null || true
}

# Scale service to specified replicas
scale_service() {
    local service_name="$1"
    local replicas="$2"
    
    log_info "Scaling $service_name to $replicas replicas..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would scale $service_name to $replicas replicas"
        return 0
    fi
    
    docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" \
        up -d --scale "$service_name=$replicas" "$service_name"
}

# Deploy new version with blue-green strategy
deploy_blue_green() {
    local service_name="$1"
    local new_image="$2"
    
    log_info "Starting blue-green deployment for $service_name with image $new_image"
    
    # Get current replica count
    local current_replicas
    current_replicas=$(docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" ps -q "$service_name" 2>/dev/null | wc -l || echo "0")
    
    if [[ $current_replicas -eq 0 ]]; then
        current_replicas=2  # Default replica count
    fi
    
    log_info "Current replicas for $service_name: $current_replicas"
    
    # Set the new image version
    export APP_VERSION="$VERSION"
    
    # Start green deployment (new version) alongside blue (current version)
    log_info "Starting green deployment (new version)..."
    scale_service "$service_name" $((current_replicas * 2))
    
    # Wait for green deployment to be healthy
    sleep 30  # Give containers time to start
    
    if ! check_service_health "$service_name"; then
        log_error "Green deployment health check failed"
        
        if [[ "$ROLLBACK_ON_FAILURE" == "true" ]]; then
            log_warn "Rolling back to blue deployment..."
            scale_service "$service_name" "$current_replicas"
            send_notification "🔄 Deployment rollback completed for $service_name" "warning"
            return 1
        else
            log_error "Deployment failed and rollback is disabled"
            return 1
        fi
    fi
    
    # Green deployment is healthy, now scale down blue deployment
    log_info "Green deployment is healthy, scaling down blue deployment..."
    scale_service "$service_name" "$current_replicas"
    
    # Final health check
    sleep 10
    if ! check_service_health "$service_name"; then
        log_error "Final health check failed after scaling down"
        
        if [[ "$ROLLBACK_ON_FAILURE" == "true" ]]; then
            log_warn "Emergency rollback..."
            # This is a simplified rollback - in production, you'd want to track the previous image
            scale_service "$service_name" "$current_replicas"
            return 1
        fi
        return 1
    fi
    
    log_success "Blue-green deployment completed successfully for $service_name"
    return 0
}

# Backup current state
backup_current_state() {
    local backup_dir="$PROJECT_ROOT/backups/deployment-$(date +%Y%m%d-%H%M%S)"
    
    log_info "Creating deployment backup..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would create backup in $backup_dir"
        return 0
    fi
    
    mkdir -p "$backup_dir"
    
    # Backup current docker-compose state
    docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" config > "$backup_dir/docker-compose-state.yml"
    
    # Backup environment variables
    if [[ -f "$ENV_FILE" ]]; then
        cp "$ENV_FILE" "$backup_dir/"
    fi
    
    # Create deployment info
    cat > "$backup_dir/deployment-info.txt" << EOF
Deployment Backup
Created: $(date)
Environment: $ENVIRONMENT
Version: $VERSION
Compose Project: $COMPOSE_PROJECT_NAME

Running Services:
$(get_running_services)

Docker Images:
$(docker-compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" images)
EOF
    
    log_success "Backup created: $backup_dir"
    echo "$backup_dir"
}

# Main deployment function
main() {
    parse_args "$@"
    
    log_info "Starting deployment..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Version: $VERSION"
    log_info "Dry Run: $DRY_RUN"
    log_info "Rollback on Failure: $ROLLBACK_ON_FAILURE"
    
    # Pre-deployment checks
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        log_error "Docker Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
    
    if [[ "$DRY_RUN" == "false" ]]; then
        # Check Docker daemon
        if ! docker info >/dev/null 2>&1; then
            log_error "Docker daemon is not running"
            exit 1
        fi
        
        # Create backup
        backup_current_state
    fi
    
    # Send start notification
    send_notification "🚀 Starting blue-green deployment for $ENVIRONMENT environment (version: $VERSION)" "info"
    
    # Deploy services
    local deployment_failed=false
    local services_to_deploy=("backend" "frontend")
    
    for service in "${services_to_deploy[@]}"; do
        log_info "Deploying $service..."
        
        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would deploy $service with version $VERSION"
            continue
        fi
        
        local image_name="ocean-shopping-center/$service:$VERSION"
        
        if ! deploy_blue_green "$service" "$image_name"; then
            log_error "Deployment failed for $service"
            deployment_failed=true
            break
        fi
        
        log_success "Successfully deployed $service"
    done
    
    # Final system health check
    if [[ "$deployment_failed" == "false" && "$DRY_RUN" == "false" ]]; then
        log_info "Running final system health check..."
        sleep 30  # Allow system to stabilize
        
        if check_external_health; then
            log_success "Deployment completed successfully!"
            send_notification "✅ Blue-green deployment completed successfully for $ENVIRONMENT environment (version: $VERSION)" "success"
        else
            log_error "Final system health check failed"
            send_notification "❌ Deployment completed but final health check failed for $ENVIRONMENT environment" "error"
            deployment_failed=true
        fi
    fi
    
    if [[ "$deployment_failed" == "true" ]]; then
        send_notification "❌ Blue-green deployment failed for $ENVIRONMENT environment (version: $VERSION)" "error"
        exit 1
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_success "Dry run completed successfully"
    fi
}

# Trap signals for cleanup
trap 'log_error "Deployment interrupted"; exit 1' INT TERM

# Run main function
main "$@"