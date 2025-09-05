#!/bin/bash

# SSL Setup Script for Ocean Shopping Center
# Initial setup and configuration of SSL certificates and related components

set -euo pipefail

# Configuration
DOMAIN="oceanshoppingcenter.com"
EMAIL="admin@oceanshoppingcenter.com"
NGINX_SSL_DIR="/etc/nginx/ssl"
CERTBOT_DIR="/etc/letsencrypt"
LOG_FILE="/var/log/ssl-setup/setup.log"

# Create log directory
mkdir -p "$(dirname "$LOG_FILE")"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
handle_error() {
    local exit_code=$1
    log "ERROR: SSL setup failed with exit code $exit_code"
    exit $exit_code
}

trap 'handle_error $?' ERR

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites for SSL setup"
    
    # Check if running as root
    if [ "$EUID" -ne 0 ]; then
        log "ERROR: This script must be run as root"
        exit 1
    fi
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        log "ERROR: Docker is not installed"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log "ERROR: Docker daemon is not running"
        exit 1
    fi
    
    # Check if docker-compose is available
    if ! command -v docker-compose &> /dev/null; then
        log "ERROR: docker-compose is not installed"
        exit 1
    fi
    
    # Check if openssl is available
    if ! command -v openssl &> /dev/null; then
        log "ERROR: OpenSSL is not installed"
        exit 1
    fi
    
    # Check DNS resolution
    if ! nslookup "$DOMAIN" &> /dev/null; then
        log "WARNING: DNS resolution for $DOMAIN failed. Certificate validation may fail."
    fi
    
    log "Prerequisites check completed successfully"
}

# Install Certbot if not already installed
install_certbot() {
    log "Checking Certbot installation"
    
    if command -v certbot &> /dev/null; then
        log "Certbot is already installed: $(certbot --version)"
        return 0
    fi
    
    log "Installing Certbot..."
    
    # Detect OS and install accordingly
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu
        apt-get update
        apt-get install -y snapd
        snap install core; snap refresh core
        snap install --classic certbot
        ln -sf /snap/bin/certbot /usr/bin/certbot
    elif [ -f /etc/redhat-release ]; then
        # RHEL/CentOS/Fedora
        yum install -y snapd
        systemctl enable --now snapd.socket
        ln -s /var/lib/snapd/snap /snap
        snap install core; snap refresh core
        snap install --classic certbot
        ln -sf /snap/bin/certbot /usr/bin/certbot
    else
        log "WARNING: Unsupported OS for automatic Certbot installation"
        log "Please install Certbot manually"
        return 1
    fi
    
    log "Certbot installed successfully: $(certbot --version)"
}

# Create SSL directory structure
create_ssl_directories() {
    log "Creating SSL directory structure"
    
    # Create nginx SSL directory
    mkdir -p "$NGINX_SSL_DIR"
    
    # Create certbot webroot directory
    mkdir -p /var/www/certbot
    
    # Create backup directory
    mkdir -p /backup/ssl-certificates
    
    # Create logs directory
    mkdir -p /var/log/certbot
    mkdir -p /var/log/ssl-monitor
    
    # Set proper permissions
    chmod 755 /var/www/certbot
    chmod 700 "$NGINX_SSL_DIR"
    chmod 755 /var/log/certbot
    chmod 755 /var/log/ssl-monitor
    
    log "SSL directory structure created successfully"
}

# Generate DH parameters
generate_dhparam() {
    local dhparam_file="$NGINX_SSL_DIR/dhparam.pem"
    
    if [ -f "$dhparam_file" ]; then
        log "DH parameters already exist at $dhparam_file"
        return 0
    fi
    
    log "Generating DH parameters (this will take several minutes)..."
    openssl dhparam -out "$dhparam_file" 4096
    chmod 600 "$dhparam_file"
    log "DH parameters generated successfully"
}

# Create self-signed certificate for initial setup
create_self_signed_cert() {
    local cert_dir="$NGINX_SSL_DIR"
    local cert_file="$cert_dir/$DOMAIN.crt"
    local key_file="$cert_dir/$DOMAIN.key"
    
    if [ -f "$cert_file" ] && [ -f "$key_file" ]; then
        log "Self-signed certificate already exists"
        return 0
    fi
    
    log "Creating self-signed certificate for initial setup"
    
    # Generate private key
    openssl genrsa -out "$key_file" 4096
    
    # Generate certificate
    openssl req -new -x509 -key "$key_file" -out "$cert_file" -days 30 -subj "/C=US/ST=State/L=City/O=Ocean Shopping Center/CN=$DOMAIN"
    
    # Set permissions
    chmod 600 "$key_file"
    chmod 644 "$cert_file"
    
    log "Self-signed certificate created (valid for 30 days)"
    log "This will be replaced with Let's Encrypt certificate"
}

# Setup SSL monitoring cron job
setup_monitoring_cron() {
    log "Setting up SSL monitoring cron job"
    
    # Create monitoring cron job
    cat > /etc/cron.d/ssl-monitoring << 'EOF'
# SSL Certificate Monitoring for Ocean Shopping Center
# Check SSL status every 4 hours
0 */4 * * * root /bin/bash /opt/ssl-config/ssl-monitor.sh monitor >> /var/log/ssl-monitor/monitor.log 2>&1

# Daily comprehensive report
0 6 * * * root /bin/bash /opt/ssl-config/ssl-monitor.sh report >> /var/log/ssl-monitor/monitor.log 2>&1
EOF
    
    chmod 644 /etc/cron.d/ssl-monitoring
    log "SSL monitoring cron job created"
}

# Copy SSL configuration files
copy_ssl_configs() {
    log "Copying SSL configuration files to system directories"
    
    # Create target directory
    mkdir -p /opt/ssl-config
    
    # Copy scripts
    cp ssl-config/certbot-renew.sh /opt/ssl-config/
    cp ssl-config/ssl-monitor.sh /opt/ssl-config/
    cp ssl-config/setup-ssl.sh /opt/ssl-config/
    
    # Make scripts executable
    chmod +x /opt/ssl-config/*.sh
    
    log "SSL configuration files copied successfully"
}

# Create security.txt file for responsible disclosure
create_security_txt() {
    local security_txt="/var/www/certbot/.well-known/security.txt"
    
    mkdir -p "$(dirname "$security_txt")"
    
    cat > "$security_txt" << EOF
Contact: security@oceanshoppingcenter.com
Expires: $(date -d '+1 year' --iso-8601)
Encryption: https://oceanshoppingcenter.com/pgp-key.txt
Acknowledgments: https://oceanshoppingcenter.com/security-acknowledgments
Policy: https://oceanshoppingcenter.com/security-policy
Hiring: https://oceanshoppingcenter.com/careers/security

# Security Contact Information for Ocean Shopping Center
# Please report security vulnerabilities responsibly
EOF
    
    log "Security.txt file created for responsible disclosure"
}

# Validate nginx configuration
validate_nginx_config() {
    log "Validating nginx configuration"
    
    if docker-compose -f docker-compose.prod.yml exec nginx-proxy nginx -t; then
        log "Nginx configuration is valid"
        return 0
    else
        log "ERROR: Nginx configuration is invalid"
        return 1
    fi
}

# Initial certificate setup
initial_cert_setup() {
    log "Starting initial certificate setup"
    
    # Start nginx with self-signed certificate
    log "Starting nginx with self-signed certificate"
    docker-compose -f docker-compose.prod.yml up -d nginx-proxy
    
    # Wait for nginx to start
    sleep 10
    
    # Test nginx is responding
    if curl -k -s "https://$DOMAIN" > /dev/null 2>&1; then
        log "Nginx is responding with self-signed certificate"
    else
        log "WARNING: Nginx not responding, certificate validation may fail"
    fi
    
    # Run certbot to get Let's Encrypt certificate
    log "Obtaining Let's Encrypt certificate"
    /opt/ssl-config/certbot-renew.sh init
    
    log "Initial certificate setup completed"
}

# Setup log rotation
setup_log_rotation() {
    log "Setting up log rotation for SSL logs"
    
    cat > /etc/logrotate.d/ssl-certificates << 'EOF'
/var/log/certbot/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
}

/var/log/ssl-monitor/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
}

/var/log/ssl-setup/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
}
EOF
    
    log "Log rotation configured for SSL logs"
}

# Main setup function
main() {
    log "=== Ocean Shopping Center SSL Setup Started ==="
    
    # Run setup steps
    check_prerequisites
    install_certbot
    create_ssl_directories
    copy_ssl_configs
    generate_dhparam
    create_self_signed_cert
    create_security_txt
    setup_monitoring_cron
    setup_log_rotation
    
    # Initial certificate setup
    if [ "${1:-}" != "--skip-cert" ]; then
        initial_cert_setup
    else
        log "Skipping initial certificate setup (--skip-cert flag provided)"
    fi
    
    log "=== Ocean Shopping Center SSL Setup Completed ==="
    log "Next steps:"
    log "1. Verify DNS is pointing to this server"
    log "2. Run: docker-compose -f docker-compose.prod.yml up -d"
    log "3. Test SSL: curl -I https://$DOMAIN"
    log "4. Check SSL grade: https://www.ssllabs.com/ssltest/"
}

# Execute main function
main "$@"