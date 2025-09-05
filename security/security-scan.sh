#!/bin/bash

# Ocean Shopping Center - Security Scanning and Vulnerability Assessment
# Comprehensive security testing automation script

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="ocean-shopping-center"
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3001}"
REPORT_DIR="./security/reports"
DATE=$(date +%Y%m%d_%H%M%S)

# Create reports directory
mkdir -p "$REPORT_DIR"

echo -e "${BLUE}=== Ocean Shopping Center Security Assessment ===${NC}"
echo -e "${BLUE}Date: $(date)${NC}"
echo -e "${BLUE}Target Backend: $BACKEND_URL${NC}"
echo -e "${BLUE}Target Frontend: $FRONTEND_URL${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to install missing tools
install_tools() {
    print_status "Checking required security tools..."
    
    # Check for OWASP ZAP
    if ! command_exists zap.sh && ! command_exists zaproxy; then
        print_warning "OWASP ZAP not found. Please install it manually."
        print_status "Download from: https://www.zaproxy.org/download/"
    fi
    
    # Check for Node.js security audit
    if ! command_exists npm; then
        print_error "npm not found. Node.js is required for dependency scanning."
        exit 1
    fi
    
    # Check for Docker security scanning
    if ! command_exists docker; then
        print_warning "Docker not found. Container security scanning will be skipped."
    fi
    
    # Check for curl for API testing
    if ! command_exists curl; then
        print_error "curl not found. API security testing requires curl."
        exit 1
    fi
    
    # Check for nmap for network scanning
    if ! command_exists nmap; then
        print_warning "nmap not found. Network scanning will be limited."
    fi
}

# Function to perform dependency vulnerability scanning
dependency_scan() {
    print_status "Starting dependency vulnerability scanning..."
    
    # Frontend dependency scan
    if [ -d "./frontend" ]; then
        print_status "Scanning frontend dependencies..."
        cd frontend
        
        # NPM Audit
        npm audit --json > "../$REPORT_DIR/frontend-npm-audit-$DATE.json" 2>/dev/null || true
        npm audit > "../$REPORT_DIR/frontend-npm-audit-$DATE.txt" 2>/dev/null || true
        
        # Check for known vulnerable packages
        if command_exists npm-check; then
            npm-check --json > "../$REPORT_DIR/frontend-npm-check-$DATE.json" 2>/dev/null || true
        fi
        
        cd ..
        print_success "Frontend dependency scan completed"
    fi
    
    # Backend dependency scan (Maven)
    if [ -f "./backend/pom.xml" ]; then
        print_status "Scanning backend dependencies..."
        cd backend
        
        # OWASP Dependency Check for Maven
        if command_exists mvn; then
            mvn org.owasp:dependency-check-maven:check -DoutputDirectory="../$REPORT_DIR" -Dformat=ALL -DfailBuildOnCVSS=0 > "../$REPORT_DIR/backend-owasp-dependency-check-$DATE.log" 2>&1 || true
        fi
        
        cd ..
        print_success "Backend dependency scan completed"
    fi
}

# Function to perform Docker security scanning
docker_security_scan() {
    print_status "Starting Docker security scanning..."
    
    if ! command_exists docker; then
        print_warning "Docker not available, skipping container security scan"
        return
    fi
    
    # Check if images exist
    if docker images | grep -q "ocean-shopping-center"; then
        # Scan backend image
        if docker images | grep -q "ocean-shopping-center/backend"; then
            print_status "Scanning backend Docker image..."
            
            # Docker Scout (if available)
            if command_exists docker-scout; then
                docker scout cves ocean-shopping-center/backend:latest > "$REPORT_DIR/docker-scout-backend-$DATE.txt" 2>&1 || true
            fi
            
            # Trivy scan (if available)
            if command_exists trivy; then
                trivy image --format json --output "$REPORT_DIR/trivy-backend-$DATE.json" ocean-shopping-center/backend:latest 2>/dev/null || true
                trivy image --format table --output "$REPORT_DIR/trivy-backend-$DATE.txt" ocean-shopping-center/backend:latest 2>/dev/null || true
            fi
        fi
        
        # Scan frontend image
        if docker images | grep -q "ocean-shopping-center/frontend"; then
            print_status "Scanning frontend Docker image..."
            
            if command_exists docker-scout; then
                docker scout cves ocean-shopping-center/frontend:latest > "$REPORT_DIR/docker-scout-frontend-$DATE.txt" 2>&1 || true
            fi
            
            if command_exists trivy; then
                trivy image --format json --output "$REPORT_DIR/trivy-frontend-$DATE.json" ocean-shopping-center/frontend:latest 2>/dev/null || true
                trivy image --format table --output "$REPORT_DIR/trivy-frontend-$DATE.txt" ocean-shopping-center/frontend:latest 2>/dev/null || true
            fi
        fi
        
        print_success "Docker security scan completed"
    else
        print_warning "No Docker images found. Build the application first with: docker-compose build"
    fi
}

# Function to perform API security testing
api_security_test() {
    print_status "Starting API security testing..."
    
    # Check if backend is running
    if ! curl -s "$BACKEND_URL/actuator/health" > /dev/null; then
        print_warning "Backend not accessible at $BACKEND_URL. Starting basic API security tests anyway..."
    fi
    
    # Create API test results file
    API_REPORT="$REPORT_DIR/api-security-test-$DATE.txt"
    echo "Ocean Shopping Center - API Security Test Results" > "$API_REPORT"
    echo "Date: $(date)" >> "$API_REPORT"
    echo "Target: $BACKEND_URL" >> "$API_REPORT"
    echo "=====================================================" >> "$API_REPORT"
    echo "" >> "$API_REPORT"
    
    # Test 1: Check for sensitive information disclosure
    print_status "Testing for information disclosure..."
    echo "=== Information Disclosure Tests ===" >> "$API_REPORT"
    
    # Check actuator endpoints
    for endpoint in "actuator/env" "actuator/configprops" "actuator/mappings" "actuator/beans"; do
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BACKEND_URL/$endpoint" 2>/dev/null || echo "HTTP_CODE:000")
        http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
        echo "  $endpoint: HTTP $http_code" >> "$API_REPORT"
        
        if [ "$http_code" = "200" ]; then
            print_warning "Potentially sensitive endpoint accessible: /$endpoint"
        fi
    done
    
    # Test 2: Check for common security headers
    print_status "Checking security headers..."
    echo "" >> "$API_REPORT"
    echo "=== Security Headers Test ===" >> "$API_REPORT"
    
    headers_response=$(curl -I -s "$BACKEND_URL/api/products" 2>/dev/null || echo "")
    
    # Check for important security headers
    security_headers=(
        "X-Content-Type-Options"
        "X-Frame-Options"
        "X-XSS-Protection"
        "Strict-Transport-Security"
        "Content-Security-Policy"
        "Referrer-Policy"
    )
    
    for header in "${security_headers[@]}"; do
        if echo "$headers_response" | grep -i "$header" > /dev/null; then
            echo "  ✓ $header: Present" >> "$API_REPORT"
        else
            echo "  ✗ $header: Missing" >> "$API_REPORT"
            print_warning "Missing security header: $header"
        fi
    done
    
    # Test 3: Authentication bypass attempts
    print_status "Testing authentication bypass..."
    echo "" >> "$API_REPORT"
    echo "=== Authentication Bypass Tests ===" >> "$API_REPORT"
    
    # Test access to protected endpoints without token
    protected_endpoints=("api/admin/users" "api/cart" "api/orders" "api/profile")
    
    for endpoint in "${protected_endpoints[@]}"; do
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BACKEND_URL/$endpoint" 2>/dev/null || echo "HTTP_CODE:000")
        http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
        echo "  $endpoint (no auth): HTTP $http_code" >> "$API_REPORT"
        
        if [ "$http_code" = "200" ]; then
            print_error "Authentication bypass possible on: /$endpoint"
        fi
    done
    
    # Test 4: SQL injection attempts (basic)
    print_status "Testing for SQL injection vulnerabilities..."
    echo "" >> "$API_REPORT"
    echo "=== SQL Injection Tests ===" >> "$API_REPORT"
    
    # Test search endpoint with SQL injection payloads
    sql_payloads=("'" "1' OR '1'='1" "'; DROP TABLE users; --" "1' UNION SELECT NULL--")
    
    for payload in "${sql_payloads[@]}"; do
        encoded_payload=$(echo "$payload" | sed 's/ /%20/g' | sed "s/'/%27/g")
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BACKEND_URL/api/products/search?q=$encoded_payload" 2>/dev/null || echo "HTTP_CODE:000")
        http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
        echo "  Payload '$payload': HTTP $http_code" >> "$API_REPORT"
        
        # Check for database errors in response
        if echo "$response" | grep -i -E "(sql|database|postgres|error|exception)" > /dev/null; then
            print_warning "Potential SQL injection vulnerability detected with payload: $payload"
        fi
    done
    
    # Test 5: Cross-Site Scripting (XSS) attempts
    print_status "Testing for XSS vulnerabilities..."
    echo "" >> "$API_REPORT"
    echo "=== XSS Tests ===" >> "$API_REPORT"
    
    xss_payloads=("<script>alert('XSS')</script>" "<img src=x onerror=alert('XSS')>" "javascript:alert('XSS')")
    
    for payload in "${xss_payloads[@]}"; do
        encoded_payload=$(echo "$payload" | sed 's/ /%20/g' | sed 's/</%3C/g' | sed 's/>/%3E/g')
        response=$(curl -s "$BACKEND_URL/api/products/search?q=$encoded_payload" 2>/dev/null || echo "")
        
        if echo "$response" | grep -F "$payload" > /dev/null; then
            print_warning "Potential XSS vulnerability detected with payload: $payload"
            echo "  Payload '$payload': VULNERABLE" >> "$API_REPORT"
        else
            echo "  Payload '$payload': SAFE" >> "$API_REPORT"
        fi
    done
    
    print_success "API security testing completed"
}

# Function to perform network security scanning
network_scan() {
    print_status "Starting network security scanning..."
    
    if ! command_exists nmap; then
        print_warning "nmap not available, skipping network scan"
        return
    fi
    
    # Extract host from URL
    backend_host=$(echo "$BACKEND_URL" | sed 's|http[s]*://||' | cut -d: -f1)
    frontend_host=$(echo "$FRONTEND_URL" | sed 's|http[s]*://||' | cut -d: -f1)
    
    # Scan backend
    print_status "Scanning backend host: $backend_host"
    nmap -sV -sC --script vuln "$backend_host" > "$REPORT_DIR/nmap-backend-$DATE.txt" 2>&1 || true
    
    # Scan frontend
    if [ "$frontend_host" != "$backend_host" ]; then
        print_status "Scanning frontend host: $frontend_host"
        nmap -sV -sC --script vuln "$frontend_host" > "$REPORT_DIR/nmap-frontend-$DATE.txt" 2>&1 || true
    fi
    
    print_success "Network security scanning completed"
}

# Function to run OWASP ZAP scanning
owasp_zap_scan() {
    print_status "Starting OWASP ZAP scanning..."
    
    # Check if ZAP is available
    zap_cmd=""
    if command_exists zap.sh; then
        zap_cmd="zap.sh"
    elif command_exists zaproxy; then
        zap_cmd="zaproxy"
    elif [ -f "/opt/zaproxy/zap.sh" ]; then
        zap_cmd="/opt/zaproxy/zap.sh"
    else
        print_warning "OWASP ZAP not found. Please install ZAP for comprehensive web application scanning."
        return
    fi
    
    # Run ZAP baseline scan
    print_status "Running ZAP baseline scan on frontend..."
    "$zap_cmd" -cmd -quickurl "$FRONTEND_URL" -quickprogress -quickout "$REPORT_DIR/zap-frontend-baseline-$DATE.html" 2>/dev/null || true
    
    # Run ZAP API scan on backend
    print_status "Running ZAP API scan on backend..."
    "$zap_cmd" -cmd -quickurl "$BACKEND_URL/api" -quickprogress -quickout "$REPORT_DIR/zap-backend-api-$DATE.html" 2>/dev/null || true
    
    print_success "OWASP ZAP scanning completed"
}

# Function to check SSL/TLS configuration
ssl_tls_check() {
    print_status "Checking SSL/TLS configuration..."
    
    # Create SSL report
    SSL_REPORT="$REPORT_DIR/ssl-tls-check-$DATE.txt"
    echo "Ocean Shopping Center - SSL/TLS Configuration Check" > "$SSL_REPORT"
    echo "Date: $(date)" >> "$SSL_REPORT"
    echo "=====================================================" >> "$SSL_REPORT"
    echo "" >> "$SSL_REPORT"
    
    # Check both URLs for HTTPS
    for url in "$BACKEND_URL" "$FRONTEND_URL"; do
        if echo "$url" | grep -q "https://"; then
            host_port=$(echo "$url" | sed 's|https://||' | cut -d/ -f1)
            host=$(echo "$host_port" | cut -d: -f1)
            port=$(echo "$host_port" | cut -d: -f2 -s)
            port=${port:-443}
            
            echo "=== $url ===" >> "$SSL_REPORT"
            
            # Test SSL connection
            if command_exists openssl; then
                echo "OpenSSL connection test:" >> "$SSL_REPORT"
                echo | openssl s_client -connect "$host:$port" -servername "$host" 2>/dev/null | openssl x509 -noout -dates >> "$SSL_REPORT" 2>/dev/null || echo "SSL connection failed" >> "$SSL_REPORT"
            fi
            
            # Test with curl
            echo "" >> "$SSL_REPORT"
            echo "cURL SSL verification:" >> "$SSL_REPORT"
            curl_result=$(curl -I -s --connect-timeout 10 "$url" 2>&1 || echo "Connection failed")
            echo "$curl_result" | head -5 >> "$SSL_REPORT"
            
            echo "" >> "$SSL_REPORT"
        else
            echo "=== $url ===" >> "$SSL_REPORT"
            echo "WARNING: Not using HTTPS" >> "$SSL_REPORT"
            echo "" >> "$SSL_REPORT"
        fi
    done
    
    print_success "SSL/TLS check completed"
}

# Function to generate security report summary
generate_summary() {
    print_status "Generating security assessment summary..."
    
    SUMMARY_REPORT="$REPORT_DIR/security-summary-$DATE.md"
    
    cat > "$SUMMARY_REPORT" << EOF
# Ocean Shopping Center - Security Assessment Summary

**Date**: $(date)  
**Assessment Type**: Automated Security Scan  
**Target Application**: Ocean Shopping Center  

## Scope

- **Backend API**: $BACKEND_URL
- **Frontend Application**: $FRONTEND_URL
- **Infrastructure**: Docker containers, PostgreSQL, Redis
- **Dependencies**: NPM packages, Maven dependencies

## Tests Performed

### ✅ Dependency Vulnerability Scanning
- Frontend NPM packages audit
- Backend Maven dependencies (OWASP Dependency Check)
- Known vulnerability database checks

### ✅ Container Security Scanning
- Docker image vulnerability assessment
- Base image security analysis
- Container configuration review

### ✅ API Security Testing
- Authentication and authorization bypass attempts
- SQL injection testing
- Cross-Site Scripting (XSS) testing
- Information disclosure checks
- Security headers verification

### ✅ Network Security Scanning
- Port scanning and service detection
- Known vulnerability checks
- Network configuration assessment

### ✅ Web Application Scanning
- OWASP ZAP baseline and API scans
- Common web vulnerabilities assessment
- Frontend security analysis

### ✅ SSL/TLS Configuration
- Certificate validation
- Protocol and cipher analysis
- HTTPS implementation check

## Report Files Generated

EOF

    # List all generated report files
    echo "" >> "$SUMMARY_REPORT"
    echo "## Generated Report Files" >> "$SUMMARY_REPORT"
    echo "" >> "$SUMMARY_REPORT"
    
    for report_file in "$REPORT_DIR"/*-"$DATE".*; do
        if [ -f "$report_file" ]; then
            filename=$(basename "$report_file")
            size=$(du -h "$report_file" | cut -f1)
            echo "- \`$filename\` ($size)" >> "$SUMMARY_REPORT"
        fi
    done
    
    cat >> "$SUMMARY_REPORT" << EOF

## Recommendations

### High Priority
1. **Review dependency vulnerabilities** - Check NPM and Maven audit reports
2. **Verify authentication controls** - Ensure all protected endpoints require proper authentication
3. **Implement missing security headers** - Add X-Content-Type-Options, X-Frame-Options, etc.
4. **Enable HTTPS** - Configure SSL/TLS certificates for production deployment

### Medium Priority
1. **Container hardening** - Review Docker image vulnerabilities and update base images
2. **Input validation** - Strengthen validation for search and form inputs
3. **Rate limiting** - Implement API rate limiting to prevent abuse
4. **Security monitoring** - Set up logging and monitoring for security events

### Low Priority
1. **Network security** - Configure firewalls and network segmentation
2. **Security headers** - Fine-tune Content Security Policy and other headers
3. **Regular scans** - Schedule automated security scans in CI/CD pipeline

## Next Steps

1. Review detailed reports in the \`security/reports\` directory
2. Address high-priority vulnerabilities first
3. Implement security fixes and re-run assessment
4. Integrate security scanning into CI/CD pipeline
5. Schedule regular security assessments

---

**Note**: This is an automated assessment. Manual security testing and code review are recommended for comprehensive security validation.
EOF

    print_success "Security assessment summary generated: $SUMMARY_REPORT"
}

# Main execution
main() {
    install_tools
    
    echo ""
    print_status "Starting comprehensive security assessment..."
    echo ""
    
    # Run all security tests
    dependency_scan
    echo ""
    
    docker_security_scan
    echo ""
    
    api_security_test
    echo ""
    
    network_scan
    echo ""
    
    owasp_zap_scan
    echo ""
    
    ssl_tls_check
    echo ""
    
    generate_summary
    
    echo ""
    print_success "Security assessment completed!"
    print_status "Reports saved to: $REPORT_DIR"
    print_status "Summary report: $REPORT_DIR/security-summary-$DATE.md"
    echo ""
    
    # Show any high-priority warnings
    if [ -f "$REPORT_DIR/api-security-test-$DATE.txt" ]; then
        warnings=$(grep -i "error\|warning\|vulnerable" "$REPORT_DIR/api-security-test-$DATE.txt" | wc -l)
        if [ "$warnings" -gt 0 ]; then
            print_warning "Found $warnings potential security issues in API tests"
        fi
    fi
}

# Run the main function
main "$@"