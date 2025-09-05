#!/bin/bash

# SSL Certificate Auto-Renewal Script for Ocean Shopping Center
# This script handles Let's Encrypt certificate generation and renewal

set -euo pipefail

# Configuration
DOMAIN="oceanshoppingcenter.com"
EMAIL="admin@oceanshoppingcenter.com"
WEBROOT_PATH="/var/www/certbot"
NGINX_CONTAINER="ocean_nginx_proxy"
CERT_PATH="/etc/letsencrypt/live/$DOMAIN"
LOG_FILE="/var/log/certbot/renewal.log"
BACKUP_DIR="/backup/ssl-certificates"
NOTIFICATION_EMAIL="security@oceanshoppingcenter.com"

# Create necessary directories
mkdir -p "$WEBROOT_PATH"
mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$BACKUP_DIR"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
handle_error() {
    local exit_code=$1
    log "ERROR: Certificate renewal failed with exit code $exit_code"
    send_notification "ERROR" "SSL certificate renewal failed for $DOMAIN"
    exit $exit_code
}

trap 'handle_error $?' ERR

# Send notification function
send_notification() {
    local status=$1
    local message=$2
    
    if command -v mail &> /dev/null; then
        echo "$message" | mail -s "SSL Certificate $status: $DOMAIN" "$NOTIFICATION_EMAIL"
    fi
    
    # Log to system journal
    logger -t ssl-renewal "$status: $message"
}

# Check if certificate exists and is valid
check_certificate() {
    if [ -f "$CERT_PATH/fullchain.pem" ]; then
        local days_until_expiry
        days_until_expiry=$(openssl x509 -enddate -noout -in "$CERT_PATH/fullchain.pem" | cut -d= -f2 | xargs -I{} date -d{} +%s | xargs -I{} expr \( {} - $(date +%s) \) / 86400)
        
        log "Certificate expires in $days_until_expiry days"
        
        if [ "$days_until_expiry" -lt 30 ]; then
            log "Certificate needs renewal (expires in less than 30 days)"
            return 1
        else
            log "Certificate is still valid for $days_until_expiry days"
            return 0
        fi
    else
        log "Certificate does not exist, will obtain new certificate"
        return 1
    fi
}

# Backup existing certificate
backup_certificate() {
    if [ -f "$CERT_PATH/fullchain.pem" ]; then
        local backup_file="$BACKUP_DIR/cert-backup-$(date +%Y%m%d-%H%M%S).tar.gz"
        tar -czf "$backup_file" -C /etc/letsencrypt/live "$DOMAIN"
        log "Certificate backed up to $backup_file"
    fi
}

# Generate DH parameters if they don't exist
generate_dhparam() {
    local dhparam_file="/etc/nginx/ssl/dhparam.pem"
    
    if [ ! -f "$dhparam_file" ]; then
        log "Generating DH parameters (this may take a while)..."
        openssl dhparam -out "$dhparam_file" 4096
        log "DH parameters generated successfully"
    else
        log "DH parameters already exist"
    fi
}

# Create CA certificate bundle for OCSP stapling
create_ca_bundle() {
    local ca_bundle="/etc/nginx/ssl/ca-certs.pem"
    
    if [ -f "$CERT_PATH/chain.pem" ]; then
        cp "$CERT_PATH/chain.pem" "$ca_bundle"
        log "CA certificate bundle created for OCSP stapling"
    fi
}

# Obtain or renew certificate
obtain_certificate() {
    log "Starting certificate obtainment/renewal process"
    
    # Ensure nginx is running for webroot authentication
    if ! docker ps | grep -q "$NGINX_CONTAINER"; then
        log "Starting nginx container for certificate validation"
        docker-compose -f docker-compose.prod.yml up -d nginx-proxy
        sleep 10
    fi
    
    # Run certbot
    if [ -f "$CERT_PATH/fullchain.pem" ]; then
        log "Renewing existing certificate"
        certbot renew --webroot --webroot-path="$WEBROOT_PATH" --email="$EMAIL" --agree-tos --non-interactive --quiet
    else
        log "Obtaining new certificate"
        certbot certonly --webroot --webroot-path="$WEBROOT_PATH" --email="$EMAIL" --agree-tos --non-interactive \
            -d "$DOMAIN" -d "www.$DOMAIN"
    fi
    
    if [ $? -eq 0 ]; then
        log "Certificate obtained/renewed successfully"
        
        # Generate additional SSL components
        generate_dhparam
        create_ca_bundle
        
        # Test nginx configuration
        if docker exec "$NGINX_CONTAINER" nginx -t; then
            log "Nginx configuration test passed"
            
            # Reload nginx to use new certificate
            docker exec "$NGINX_CONTAINER" nginx -s reload
            log "Nginx reloaded with new certificate"
            
            send_notification "SUCCESS" "SSL certificate for $DOMAIN has been successfully renewed"
        else
            log "ERROR: Nginx configuration test failed"
            send_notification "ERROR" "SSL certificate renewal succeeded but nginx configuration is invalid"
            exit 1
        fi
    else
        log "ERROR: Certificate obtainment/renewal failed"
        send_notification "ERROR" "Failed to obtain/renew SSL certificate for $DOMAIN"
        exit 1
    fi
}

# Verify certificate installation
verify_certificate() {
    log "Verifying certificate installation"
    
    # Check if certificate files exist
    if [ ! -f "$CERT_PATH/fullchain.pem" ] || [ ! -f "$CERT_PATH/privkey.pem" ]; then
        log "ERROR: Certificate files not found"
        return 1
    fi
    
    # Verify certificate is valid
    if openssl x509 -in "$CERT_PATH/fullchain.pem" -text -noout > /dev/null 2>&1; then
        log "Certificate is valid"
    else
        log "ERROR: Certificate is not valid"
        return 1
    fi
    
    # Check certificate expiry
    local expiry_date
    expiry_date=$(openssl x509 -enddate -noout -in "$CERT_PATH/fullchain.pem" | cut -d= -f2)
    log "Certificate expires: $expiry_date"
    
    # Test HTTPS connectivity
    if curl -k -s "https://$DOMAIN" > /dev/null 2>&1; then
        log "HTTPS connectivity test passed"
    else
        log "WARNING: HTTPS connectivity test failed"
    fi
    
    return 0
}

# Set up auto-renewal cron job
setup_cron() {
    local cron_file="/etc/cron.d/ssl-renewal"
    
    cat > "$cron_file" << EOF
# SSL Certificate Auto-Renewal for Ocean Shopping Center
# Runs twice daily at 2 AM and 2 PM
0 2,14 * * * root /bin/bash /opt/ssl-config/certbot-renew.sh >> $LOG_FILE 2>&1
EOF
    
    chmod 644 "$cron_file"
    log "Cron job for auto-renewal has been set up"
}

# Main execution
main() {
    log "=== SSL Certificate Management Started ==="
    
    case "${1:-auto}" in
        "init")
            log "Initializing SSL certificates"
            backup_certificate
            obtain_certificate
            verify_certificate
            setup_cron
            ;;
        "renew")
            log "Manual certificate renewal requested"
            if ! check_certificate; then
                backup_certificate
                obtain_certificate
                verify_certificate
            fi
            ;;
        "auto")
            log "Automatic certificate check/renewal"
            if ! check_certificate; then
                backup_certificate
                obtain_certificate
                verify_certificate
            fi
            ;;
        "test")
            log "Testing certificate configuration"
            verify_certificate
            ;;
        "force")
            log "Forced certificate renewal"
            backup_certificate
            obtain_certificate
            verify_certificate
            ;;
        *)
            echo "Usage: $0 {init|renew|auto|test|force}"
            echo "  init  - Initial certificate setup"
            echo "  renew - Manual renewal check"
            echo "  auto  - Automatic renewal (default)"
            echo "  test  - Test current certificate"
            echo "  force - Force certificate renewal"
            exit 1
            ;;
    esac
    
    log "=== SSL Certificate Management Completed ==="
}

# Execute main function
main "$@"