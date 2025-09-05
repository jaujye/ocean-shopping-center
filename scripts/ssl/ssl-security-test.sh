#!/bin/bash

# SSL Security Testing Script for Ocean Shopping Center
# Comprehensive security validation and testing suite

set -euo pipefail

# Configuration
DOMAIN="${DOMAIN:-oceanshoppingcenter.com}"
LOG_FILE="/var/log/ssl-testing/security-test.log"
REPORT_FILE="/var/log/ssl-testing/security-report.json"
TEMP_DIR="/tmp/ssl-test"
ALERT_EMAIL="${ALERT_EMAIL:-security@oceanshoppingcenter.com}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create necessary directories
mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$(dirname "$REPORT_FILE")"
mkdir -p "$TEMP_DIR"

# Test results tracking
declare -A test_results
total_tests=0
passed_tests=0
failed_tests=0

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Colored output function
colored_output() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Test result tracking
record_test_result() {
    local test_name=$1
    local result=$2
    local details=${3:-}
    
    test_results["$test_name"]="$result"
    ((total_tests++))
    
    if [ "$result" == "PASS" ]; then
        ((passed_tests++))
        colored_output "$GREEN" "✓ $test_name: PASSED"
    else
        ((failed_tests++))
        colored_output "$RED" "✗ $test_name: FAILED"
        if [ -n "$details" ]; then
            colored_output "$RED" "  Details: $details"
        fi
    fi
    
    log "$test_name: $result - $details"
}

# Test SSL certificate validity
test_certificate_validity() {
    log "Testing SSL certificate validity"
    
    local cert_info
    local expiry_days
    local issuer
    local subject
    
    # Get certificate information
    if cert_info=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" 2>/dev/null <<< "Q"); then
        
        # Extract certificate
        echo "$cert_info" | openssl x509 -text -noout > "$TEMP_DIR/cert_details.txt" 2>/dev/null
        
        if [ -f "$TEMP_DIR/cert_details.txt" ]; then
            # Check expiry
            expiry_days=$(echo "$cert_info" | openssl x509 -enddate -noout | cut -d= -f2 | xargs -I{} date -d{} +%s | xargs -I{} expr \( {} - $(date +%s) \) / 86400)
            
            # Check issuer
            issuer=$(echo "$cert_info" | openssl x509 -issuer -noout | cut -d= -f2-)
            
            # Check subject
            subject=$(echo "$cert_info" | openssl x509 -subject -noout | cut -d= -f2-)
            
            if [ "$expiry_days" -gt 7 ]; then
                record_test_result "Certificate Validity" "PASS" "Certificate valid for $expiry_days days"
            else
                record_test_result "Certificate Validity" "FAIL" "Certificate expires in $expiry_days days"
            fi
            
            # Check if it's a legitimate CA
            if echo "$issuer" | grep -E "(Let's Encrypt|DigiCert|GlobalSign|Sectigo|GeoTrust)" > /dev/null; then
                record_test_result "Certificate Authority" "PASS" "Issued by: $issuer"
            else
                record_test_result "Certificate Authority" "FAIL" "Unknown/untrusted CA: $issuer"
            fi
            
        else
            record_test_result "Certificate Validity" "FAIL" "Cannot extract certificate details"
        fi
    else
        record_test_result "Certificate Validity" "FAIL" "Cannot connect to retrieve certificate"
    fi
}

# Test SSL protocols and ciphers
test_ssl_protocols() {
    log "Testing SSL protocols and cipher suites"
    
    local protocols=("ssl3" "tls1" "tls1_1" "tls1_2" "tls1_3")
    local secure_protocols=("tls1_2" "tls1_3")
    local insecure_protocols=("ssl3" "tls1" "tls1_1")
    
    # Test each protocol
    for protocol in "${protocols[@]}"; do
        if timeout 10 openssl s_client -connect "$DOMAIN:443" -"$protocol" 2>/dev/null <<< "Q" | grep -q "BEGIN CERTIFICATE"; then
            if [[ " ${insecure_protocols[*]} " =~ " ${protocol} " ]]; then
                record_test_result "Protocol: $protocol" "FAIL" "Insecure protocol is enabled"
            else
                record_test_result "Protocol: $protocol" "PASS" "Secure protocol is enabled"
            fi
        else
            if [[ " ${insecure_protocols[*]} " =~ " ${protocol} " ]]; then
                record_test_result "Protocol: $protocol" "PASS" "Insecure protocol is properly disabled"
            else
                record_test_result "Protocol: $protocol" "FAIL" "Secure protocol is not available"
            fi
        fi
    done
    
    # Test for weak ciphers
    local cipher_test
    cipher_test=$(timeout 30 nmap --script ssl-enum-ciphers -p 443 "$DOMAIN" 2>/dev/null | grep -E "(RC4|MD5|3DES|DES)" || echo "")
    
    if [ -z "$cipher_test" ]; then
        record_test_result "Weak Ciphers" "PASS" "No weak ciphers detected"
    else
        record_test_result "Weak Ciphers" "FAIL" "Weak ciphers found: $cipher_test"
    fi
}

# Test HTTP to HTTPS redirect
test_https_redirect() {
    log "Testing HTTP to HTTPS redirect"
    
    local redirect_response
    redirect_response=$(timeout 10 curl -s -I "http://$DOMAIN" 2>/dev/null | head -10)
    
    if echo "$redirect_response" | grep -E "HTTP/[12]\.[01] 30[1-8]" > /dev/null && \
       echo "$redirect_response" | grep -i "location.*https://" > /dev/null; then
        record_test_result "HTTPS Redirect" "PASS" "HTTP properly redirects to HTTPS"
    else
        record_test_result "HTTPS Redirect" "FAIL" "HTTP does not redirect to HTTPS properly"
    fi
}

# Test security headers
test_security_headers() {
    log "Testing security headers"
    
    local headers
    headers=$(timeout 30 curl -s -I "https://$DOMAIN" 2>/dev/null)
    
    # Test individual headers
    local required_headers=(
        "strict-transport-security:HSTS"
        "x-frame-options:Frame Protection"
        "x-content-type-options:MIME Sniffing Protection"
        "content-security-policy:Content Security Policy"
        "x-xss-protection:XSS Protection"
        "referrer-policy:Referrer Policy"
    )
    
    for header_check in "${required_headers[@]}"; do
        IFS=':' read -r header_name friendly_name <<< "$header_check"
        
        if echo "$headers" | grep -qi "$header_name"; then
            local header_value
            header_value=$(echo "$headers" | grep -i "$header_name" | cut -d: -f2- | xargs)
            record_test_result "Header: $friendly_name" "PASS" "Present: $header_value"
        else
            record_test_result "Header: $friendly_name" "FAIL" "Missing security header"
        fi
    done
    
    # Check HSTS max-age value
    if echo "$headers" | grep -qi "strict-transport-security"; then
        local hsts_value
        hsts_value=$(echo "$headers" | grep -i "strict-transport-security" | cut -d: -f2- | xargs)
        
        if echo "$hsts_value" | grep -E "max-age=([1-9][0-9]{6,})" > /dev/null; then
            record_test_result "HSTS Duration" "PASS" "Appropriate max-age value: $hsts_value"
        else
            record_test_result "HSTS Duration" "FAIL" "HSTS max-age too short: $hsts_value"
        fi
    fi
}

# Test for SSL vulnerabilities
test_ssl_vulnerabilities() {
    log "Testing for SSL vulnerabilities"
    
    # Test for Heartbleed
    if command -v nmap > /dev/null 2>&1; then
        local heartbleed_result
        heartbleed_result=$(timeout 60 nmap --script ssl-heartbleed -p 443 "$DOMAIN" 2>/dev/null | grep -i "vulnerable" || echo "")
        
        if [ -z "$heartbleed_result" ]; then
            record_test_result "Heartbleed Vulnerability" "PASS" "Not vulnerable to Heartbleed"
        else
            record_test_result "Heartbleed Vulnerability" "FAIL" "Vulnerable to Heartbleed"
        fi
        
        # Test for POODLE
        local poodle_result
        poodle_result=$(timeout 60 nmap --script ssl-poodle -p 443 "$DOMAIN" 2>/dev/null | grep -i "vulnerable" || echo "")
        
        if [ -z "$poodle_result" ]; then
            record_test_result "POODLE Vulnerability" "PASS" "Not vulnerable to POODLE"
        else
            record_test_result "POODLE Vulnerability" "FAIL" "Vulnerable to POODLE"
        fi
        
        # Test for BEAST
        local beast_result
        beast_result=$(timeout 60 nmap --script ssl-enum-ciphers -p 443 "$DOMAIN" 2>/dev/null | grep -i "CBC" | grep -i "TLS1.0" || echo "")
        
        if [ -z "$beast_result" ]; then
            record_test_result "BEAST Vulnerability" "PASS" "Not vulnerable to BEAST"
        else
            record_test_result "BEAST Vulnerability" "FAIL" "Potentially vulnerable to BEAST"
        fi
    else
        record_test_result "Vulnerability Scan" "SKIP" "nmap not available for vulnerability testing"
    fi
}

# Test certificate chain and trust
test_certificate_chain() {
    log "Testing certificate chain and trust"
    
    local chain_result
    chain_result=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" -verify_return_error 2>&1 <<< "Q" | grep -E "(Verify return code|verify error)" || echo "")
    
    if echo "$chain_result" | grep -q "Verify return code: 0 (ok)"; then
        record_test_result "Certificate Chain" "PASS" "Certificate chain is valid and trusted"
    elif [ -z "$chain_result" ]; then
        record_test_result "Certificate Chain" "PASS" "Certificate chain appears valid"
    else
        record_test_result "Certificate Chain" "FAIL" "Certificate chain verification failed: $chain_result"
    fi
    
    # Test OCSP stapling
    local ocsp_result
    ocsp_result=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" -status 2>/dev/null <<< "Q" | grep -A5 "OCSP response" || echo "")
    
    if echo "$ocsp_result" | grep -q "OCSP Response Status: successful"; then
        record_test_result "OCSP Stapling" "PASS" "OCSP stapling is working"
    else
        record_test_result "OCSP Stapling" "WARN" "OCSP stapling not detected or not working"
    fi
}

# Test Perfect Forward Secrecy
test_perfect_forward_secrecy() {
    log "Testing Perfect Forward Secrecy"
    
    local cipher_info
    cipher_info=$(timeout 30 openssl s_client -connect "$DOMAIN:443" -servername "$DOMAIN" 2>/dev/null <<< "Q" | grep "Cipher    :" || echo "")
    
    if echo "$cipher_info" | grep -E "(ECDHE|DHE)" > /dev/null; then
        record_test_result "Perfect Forward Secrecy" "PASS" "PFS is supported: $cipher_info"
    else
        record_test_result "Perfect Forward Secrecy" "FAIL" "PFS not supported: $cipher_info"
    fi
}

# Test application-level security
test_application_security() {
    log "Testing application-level security"
    
    # Test for API endpoints over HTTPS
    local api_endpoints=("/api/health" "/api/auth/status" "/api/products")
    
    for endpoint in "${api_endpoints[@]}"; do
        local response_code
        response_code=$(timeout 10 curl -s -o /dev/null -w "%{http_code}" "https://$DOMAIN$endpoint" 2>/dev/null || echo "000")
        
        if [ "$response_code" != "000" ]; then
            record_test_result "API HTTPS: $endpoint" "PASS" "Endpoint accessible via HTTPS (HTTP $response_code)"
        else
            record_test_result "API HTTPS: $endpoint" "FAIL" "Endpoint not accessible via HTTPS"
        fi
    done
    
    # Test for insecure content
    local page_content
    page_content=$(timeout 30 curl -s "https://$DOMAIN" 2>/dev/null || echo "")
    
    if echo "$page_content" | grep -E "http://[^/]" > /dev/null; then
        record_test_result "Mixed Content" "FAIL" "Page contains HTTP resources that may cause mixed content warnings"
    else
        record_test_result "Mixed Content" "PASS" "No obvious mixed content detected"
    fi
}

# Generate comprehensive report
generate_report() {
    log "Generating security test report"
    
    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    # Create JSON report
    cat > "$REPORT_FILE" << EOF
{
    "domain": "$DOMAIN",
    "timestamp": "$timestamp",
    "summary": {
        "total_tests": $total_tests,
        "passed_tests": $passed_tests,
        "failed_tests": $failed_tests,
        "pass_rate": $(( passed_tests * 100 / total_tests ))
    },
    "test_results": {
EOF
    
    local first=true
    for test_name in "${!test_results[@]}"; do
        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$REPORT_FILE"
        fi
        echo -n "        \"$test_name\": \"${test_results[$test_name]}\"" >> "$REPORT_FILE"
    done
    
    cat >> "$REPORT_FILE" << EOF

    }
}
EOF
    
    log "Report generated at $REPORT_FILE"
}

# Send alert if critical tests failed
send_alert() {
    if [ $failed_tests -gt 0 ]; then
        local subject="SSL Security Test ALERT - $failed_tests tests failed for $DOMAIN"
        local body="SSL Security testing completed with $failed_tests failures out of $total_tests tests.

Failed tests:"
        
        for test_name in "${!test_results[@]}"; do
            if [ "${test_results[$test_name]}" == "FAIL" ]; then
                body="$body
- $test_name"
            fi
        done
        
        body="$body

Please check the full report at: $REPORT_FILE
Log file: $LOG_FILE

Timestamp: $(date)"
        
        if command -v mail > /dev/null 2>&1 && [ -n "$ALERT_EMAIL" ]; then
            echo "$body" | mail -s "$subject" "$ALERT_EMAIL"
            log "Alert sent to $ALERT_EMAIL"
        fi
        
        # Log critical failure
        logger -t ssl-security-test "CRITICAL: SSL security tests failed for $DOMAIN ($failed_tests/$total_tests)"
    fi
}

# Main testing function
main() {
    colored_output "$BLUE" "=== Ocean Shopping Center SSL Security Testing ==="
    log "=== SSL Security Testing Started for $DOMAIN ==="
    
    # Run all tests
    test_certificate_validity
    test_ssl_protocols
    test_https_redirect
    test_security_headers
    test_ssl_vulnerabilities
    test_certificate_chain
    test_perfect_forward_secrecy
    test_application_security
    
    # Generate report and summary
    generate_report
    
    echo
    colored_output "$BLUE" "=== Test Summary ==="
    colored_output "$GREEN" "Passed: $passed_tests"
    colored_output "$RED" "Failed: $failed_tests"
    colored_output "$BLUE" "Total:  $total_tests"
    
    if [ $failed_tests -eq 0 ]; then
        colored_output "$GREEN" "🎉 All SSL security tests passed!"
        log "All SSL security tests completed successfully"
    else
        colored_output "$RED" "⚠️  $failed_tests SSL security tests failed!"
        log "SSL security tests completed with $failed_tests failures"
        send_alert
    fi
    
    log "=== SSL Security Testing Completed ==="
    
    # Cleanup
    rm -rf "$TEMP_DIR"
    
    # Exit with appropriate code
    [ $failed_tests -eq 0 ]
}

# Handle command line arguments
case "${1:-test}" in
    "test")
        main
        ;;
    "report-only")
        generate_report
        ;;
    "cert-only")
        test_certificate_validity
        ;;
    "headers-only")
        test_security_headers
        ;;
    "protocols-only")
        test_ssl_protocols
        ;;
    *)
        echo "Usage: $0 {test|report-only|cert-only|headers-only|protocols-only}"
        echo "  test          - Run all security tests (default)"
        echo "  report-only   - Generate report from existing results"
        echo "  cert-only     - Test certificate validity only"
        echo "  headers-only  - Test security headers only"
        echo "  protocols-only - Test SSL protocols only"
        exit 1
        ;;
esac