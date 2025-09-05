#!/bin/bash

# SSL Certificate Monitoring Script for Ocean Shopping Center
# Monitors certificate health, expiry, and security configuration

set -euo pipefail

# Configuration
DOMAIN="oceanshoppingcenter.com"
ALERT_THRESHOLD_DAYS=30
WARNING_THRESHOLD_DAYS=7
LOG_FILE="/var/log/ssl-monitor/monitor.log"
REPORT_FILE="/var/log/ssl-monitor/security-report.json"
NOTIFICATION_EMAIL="security@oceanshoppingcenter.com"
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"

# Create log directory
mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$(dirname "$REPORT_FILE")"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Send notification
send_notification() {
    local level=$1
    local title=$2
    local message=$3
    
    log "$level: $title - $message"
    
    # Email notification
    if command -v mail &> /dev/null && [ -n "$NOTIFICATION_EMAIL" ]; then
        echo -e "Subject: SSL Monitor Alert: $title\n\n$message\n\nDomain: $DOMAIN\nTimestamp: $(date)\nLevel: $level" | \
            mail -s "SSL Monitor Alert: $title" "$NOTIFICATION_EMAIL"
    fi
    
    # Slack notification
    if [ -n "$SLACK_WEBHOOK_URL" ]; then
        local color="good"
        case $level in
            "CRITICAL") color="danger" ;;
            "WARNING") color="warning" ;;
            "ERROR") color="danger" ;;
        esac
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"attachments\":[{\"color\":\"$color\",\"title\":\"SSL Monitor Alert: $title\",\"text\":\"$message\",\"fields\":[{\"title\":\"Domain\",\"value\":\"$DOMAIN\",\"short\":true},{\"title\":\"Level\",\"value\":\"$level\",\"short\":true}]}]}" \
            "$SLACK_WEBHOOK_URL" 2>/dev/null || true
    fi
    
    # System logger
    logger -t ssl-monitor "$level: $title - $message"
}

# Check certificate expiry
check_certificate_expiry() {
    log "Checking certificate expiry for $DOMAIN"
    
    local cert_path="/etc/letsencrypt/live/$DOMAIN/fullchain.pem"
    
    if [ ! -f "$cert_path" ]; then
        send_notification "CRITICAL" "Certificate Missing" "SSL certificate file not found at $cert_path"
        return 1
    fi
    
    # Get certificate expiry information
    local expiry_date expiry_timestamp current_timestamp days_until_expiry
    
    expiry_date=$(openssl x509 -enddate -noout -in "$cert_path" | cut -d= -f2)
    expiry_timestamp=$(date -d "$expiry_date" +%s)
    current_timestamp=$(date +%s)
    days_until_expiry=$(( (expiry_timestamp - current_timestamp) / 86400 ))
    
    log "Certificate expires in $days_until_expiry days ($expiry_date)"
    
    # Check thresholds and send alerts
    if [ "$days_until_expiry" -lt 0 ]; then
        send_notification "CRITICAL" "Certificate Expired" "SSL certificate for $DOMAIN has EXPIRED on $expiry_date"
        return 1
    elif [ "$days_until_expiry" -lt "$WARNING_THRESHOLD_DAYS" ]; then
        send_notification "CRITICAL" "Certificate Expiring Soon" "SSL certificate for $DOMAIN expires in $days_until_expiry days on $expiry_date"
        return 1
    elif [ "$days_until_expiry" -lt "$ALERT_THRESHOLD_DAYS" ]; then
        send_notification "WARNING" "Certificate Renewal Needed" "SSL certificate for $DOMAIN expires in $days_until_expiry days on $expiry_date"
    else
        log "Certificate expiry check passed ($days_until_expiry days remaining)"
    fi
    
    return 0
}

# Test SSL/TLS connectivity
test_ssl_connectivity() {
    log "Testing SSL/TLS connectivity for $DOMAIN"
    
    local ssl_test_result
    ssl_test_result=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" -verify_return_error 2>&1 <<< "Q" || true)
    
    if echo "$ssl_test_result" | grep -q "Verify return code: 0 (ok)"; then
        log "SSL connectivity test passed"
        return 0
    else
        send_notification "ERROR" "SSL Connectivity Failed" "SSL connectivity test failed for $DOMAIN. Error: $(echo "$ssl_test_result" | tail -5)"
        return 1
    fi
}

# Check SSL configuration security
check_ssl_security() {
    log "Checking SSL security configuration for $DOMAIN"
    
    local security_issues=()
    
    # Test with testssl.sh if available
    if command -v testssl.sh &> /dev/null; then
        log "Running comprehensive SSL security test with testssl.sh"
        
        local testssl_output
        testssl_output=$(timeout 300 testssl.sh --quiet --jsonfile-pretty "$REPORT_FILE.tmp" "https://$DOMAIN" 2>&1 || true)
        
        if [ -f "$REPORT_FILE.tmp" ]; then
            mv "$REPORT_FILE.tmp" "$REPORT_FILE"
            
            # Parse results for critical issues
            if grep -q '"severity": "CRITICAL"' "$REPORT_FILE"; then
                security_issues+=("Critical SSL vulnerabilities detected")
            fi
            
            if grep -q '"severity": "HIGH"' "$REPORT_FILE"; then
                security_issues+=("High severity SSL issues detected")
            fi
        fi
    else
        log "testssl.sh not available, performing basic SSL checks"
        
        # Basic SSL protocol check
        local ssl_protocols
        ssl_protocols=$(timeout 30 nmap --script ssl-enum-ciphers -p 443 "$DOMAIN" 2>/dev/null | grep -E "(TLS|SSL)" || true)
        
        if echo "$ssl_protocols" | grep -q "SSLv3"; then
            security_issues+=("Insecure SSLv3 protocol enabled")
        fi
        
        if echo "$ssl_protocols" | grep -q "TLSv1.0"; then
            security_issues+=("Insecure TLS 1.0 protocol enabled")
        fi
        
        if echo "$ssl_protocols" | grep -q "TLSv1.1"; then
            security_issues+=("Insecure TLS 1.1 protocol enabled")
        fi
    fi
    
    # Check certificate chain
    local chain_issues
    chain_issues=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" -verify_return_error 2>&1 <<< "Q" | grep -i "verify.*error" || true)
    
    if [ -n "$chain_issues" ]; then
        security_issues+=("Certificate chain verification issues: $chain_issues")
    fi
    
    # Report security issues
    if [ ${#security_issues[@]} -gt 0 ]; then
        local issues_text
        printf -v issues_text '%s\n' "${security_issues[@]}"
        send_notification "WARNING" "SSL Security Issues Detected" "SSL security issues found for $DOMAIN:\n$issues_text"
        return 1
    else
        log "SSL security check passed"
        return 0
    fi
}

# Check HTTPS redirect
check_https_redirect() {
    log "Checking HTTP to HTTPS redirect for $DOMAIN"
    
    local redirect_response
    redirect_response=$(timeout 30 curl -s -I "http://$DOMAIN" | grep -E "^(HTTP|Location)" || true)
    
    if echo "$redirect_response" | grep -q "301\|302" && echo "$redirect_response" | grep -q "https://"; then
        log "HTTP to HTTPS redirect working correctly"
        return 0
    else
        send_notification "WARNING" "HTTPS Redirect Issue" "HTTP to HTTPS redirect not working correctly for $DOMAIN. Response: $redirect_response"
        return 1
    fi
}

# Check security headers
check_security_headers() {
    log "Checking security headers for $DOMAIN"
    
    local headers
    headers=$(timeout 30 curl -s -I "https://$DOMAIN" || true)
    
    local missing_headers=()
    
    # Check for required security headers
    if ! echo "$headers" | grep -qi "strict-transport-security"; then
        missing_headers+=("Strict-Transport-Security (HSTS)")
    fi
    
    if ! echo "$headers" | grep -qi "x-frame-options"; then
        missing_headers+=("X-Frame-Options")
    fi
    
    if ! echo "$headers" | grep -qi "x-content-type-options"; then
        missing_headers+=("X-Content-Type-Options")
    fi
    
    if ! echo "$headers" | grep -qi "content-security-policy"; then
        missing_headers+=("Content-Security-Policy")
    fi
    
    if ! echo "$headers" | grep -qi "x-xss-protection"; then
        missing_headers+=("X-XSS-Protection")
    fi
    
    if ! echo "$headers" | grep -qi "referrer-policy"; then
        missing_headers+=("Referrer-Policy")
    fi
    
    if [ ${#missing_headers[@]} -gt 0 ]; then
        local missing_text
        printf -v missing_text '%s, ' "${missing_headers[@]}"
        missing_text=${missing_text%, }
        send_notification "WARNING" "Missing Security Headers" "Missing security headers for $DOMAIN: $missing_text"
        return 1
    else
        log "All required security headers are present"
        return 0
    fi
}

# Check certificate transparency logs
check_certificate_transparency() {
    log "Checking certificate transparency logs for $DOMAIN"
    
    # Use crt.sh API to check for certificate transparency
    local ct_response
    ct_response=$(timeout 30 curl -s "https://crt.sh/?q=$DOMAIN&output=json" | jq -r '.[0].id' 2>/dev/null || echo "error")
    
    if [ "$ct_response" != "error" ] && [ "$ct_response" != "null" ] && [ -n "$ct_response" ]; then
        log "Certificate found in transparency logs (ID: $ct_response)"
        return 0
    else
        send_notification "WARNING" "Certificate Transparency Issue" "Certificate for $DOMAIN not found in transparency logs or API error"
        return 1
    fi
}

# Generate monitoring report
generate_report() {
    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    cat > "${REPORT_FILE%.json}-summary.json" << EOF
{
    "domain": "$DOMAIN",
    "timestamp": "$timestamp",
    "checks": {
        "certificate_expiry": $(check_certificate_expiry >/dev/null 2>&1 && echo "true" || echo "false"),
        "ssl_connectivity": $(test_ssl_connectivity >/dev/null 2>&1 && echo "true" || echo "false"),
        "ssl_security": $(check_ssl_security >/dev/null 2>&1 && echo "true" || echo "false"),
        "https_redirect": $(check_https_redirect >/dev/null 2>&1 && echo "true" || echo "false"),
        "security_headers": $(check_security_headers >/dev/null 2>&1 && echo "true" || echo "false"),
        "certificate_transparency": $(check_certificate_transparency >/dev/null 2>&1 && echo "true" || echo "false")
    }
}
EOF
    
    log "Monitoring report generated at ${REPORT_FILE%.json}-summary.json"
}

# Main monitoring function
main() {
    log "=== SSL Certificate Monitoring Started ==="
    
    local overall_status=0
    
    # Run all checks
    check_certificate_expiry || overall_status=1
    test_ssl_connectivity || overall_status=1
    check_ssl_security || overall_status=1
    check_https_redirect || overall_status=1
    check_security_headers || overall_status=1
    check_certificate_transparency || overall_status=1
    
    # Generate report
    generate_report
    
    if [ $overall_status -eq 0 ]; then
        log "All SSL monitoring checks passed"
        send_notification "INFO" "SSL Monitoring Complete" "All SSL checks passed for $DOMAIN"
    else
        log "Some SSL monitoring checks failed"
        send_notification "WARNING" "SSL Monitoring Issues" "Some SSL monitoring checks failed for $DOMAIN. Check logs for details."
    fi
    
    log "=== SSL Certificate Monitoring Completed ==="
    return $overall_status
}

# Handle command line arguments
case "${1:-monitor}" in
    "monitor")
        main
        ;;
    "expiry")
        check_certificate_expiry
        ;;
    "connectivity")
        test_ssl_connectivity
        ;;
    "security")
        check_ssl_security
        ;;
    "headers")
        check_security_headers
        ;;
    "report")
        generate_report
        ;;
    *)
        echo "Usage: $0 {monitor|expiry|connectivity|security|headers|report}"
        echo "  monitor      - Run all monitoring checks (default)"
        echo "  expiry       - Check certificate expiry only"
        echo "  connectivity - Test SSL connectivity only"
        echo "  security     - Check SSL security configuration"
        echo "  headers      - Check security headers"
        echo "  report       - Generate monitoring report"
        exit 1
        ;;
esac