#!/bin/bash

# SSL Labs Grade Checker for Ocean Shopping Center
# Automated SSL Labs API testing and grade monitoring

set -euo pipefail

# Configuration
DOMAIN="${DOMAIN:-oceanshoppingcenter.com}"
API_BASE="https://api.ssllabs.com/api/v3"
LOG_FILE="/var/log/ssl-grading/grade-check.log"
REPORT_FILE="/var/log/ssl-grading/grade-report.json"
MIN_GRADE="${MIN_GRADE:-A}"
ALERT_EMAIL="${ALERT_EMAIL:-security@oceanshoppingcenter.com}"
MAX_WAIT_TIME=600  # 10 minutes max wait
CHECK_INTERVAL=30  # Check every 30 seconds

# Grade scoring for comparison
declare -A GRADE_SCORES=(
    ["A+"]="100"
    ["A"]="90"
    ["A-"]="85"
    ["B"]="80"
    ["C"]="70"
    ["D"]="60"
    ["E"]="50"
    ["F"]="40"
    ["T"]="30"  # Certificate name mismatch
    ["M"]="20"  # Certificate missing
)

# Create log directories
mkdir -p "$(dirname "$LOG_FILE")"
mkdir -p "$(dirname "$REPORT_FILE")"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Error handling
handle_error() {
    local exit_code=$1
    log "ERROR: SSL grade check failed with exit code $exit_code"
    exit $exit_code
}

trap 'handle_error $?' ERR

# Send notification
send_notification() {
    local level=$1
    local subject=$2
    local message=$3
    
    log "$level: $subject - $message"
    
    if command -v mail &> /dev/null && [ -n "$ALERT_EMAIL" ]; then
        echo -e "$message\n\nDomain: $DOMAIN\nTimestamp: $(date)" | \
            mail -s "SSL Grade Alert: $subject" "$ALERT_EMAIL"
    fi
    
    logger -t ssl-grade-check "$level: $subject - $message"
}

# Check API availability
check_api_availability() {
    log "Checking SSL Labs API availability"
    
    local api_info
    api_info=$(curl -s "$API_BASE/info" 2>/dev/null || echo "")
    
    if [ -z "$api_info" ]; then
        log "ERROR: SSL Labs API is not accessible"
        return 1
    fi
    
    # Check if API is in maintenance mode
    if echo "$api_info" | jq -r '.message' 2>/dev/null | grep -qi "maintenance"; then
        log "WARNING: SSL Labs API is in maintenance mode"
        return 1
    fi
    
    # Get rate limits
    local current_assessments max_assessments
    current_assessments=$(echo "$api_info" | jq -r '.currentAssessments' 2>/dev/null || echo "0")
    max_assessments=$(echo "$api_info" | jq -r '.maxAssessments' 2>/dev/null || echo "25")
    
    log "SSL Labs API available - Current assessments: $current_assessments/$max_assessments"
    
    if [ "$current_assessments" -ge "$max_assessments" ]; then
        log "WARNING: SSL Labs API assessment queue is full"
        return 1
    fi
    
    return 0
}

# Start SSL assessment
start_assessment() {
    log "Starting SSL assessment for $DOMAIN"
    
    local start_response
    start_response=$(curl -s "$API_BASE/analyze?host=$DOMAIN&publish=off&startNew=on&all=done&ignoreMismatch=on" 2>/dev/null || echo "")
    
    if [ -z "$start_response" ]; then
        log "ERROR: Failed to start SSL assessment"
        return 1
    fi
    
    # Check for errors
    local errors
    errors=$(echo "$start_response" | jq -r '.errors[]?.message' 2>/dev/null | tr '\n' ' ' || echo "")
    
    if [ -n "$errors" ]; then
        log "ERROR: SSL assessment failed to start: $errors"
        return 1
    fi
    
    local status
    status=$(echo "$start_response" | jq -r '.status' 2>/dev/null || echo "ERROR")
    
    log "Assessment started with status: $status"
    return 0
}

# Check assessment progress
check_assessment_progress() {
    local elapsed_time=0
    
    log "Monitoring assessment progress (max wait: ${MAX_WAIT_TIME}s)"
    
    while [ $elapsed_time -lt $MAX_WAIT_TIME ]; do
        local progress_response
        progress_response=$(curl -s "$API_BASE/analyze?host=$DOMAIN&all=done" 2>/dev/null || echo "")
        
        if [ -z "$progress_response" ]; then
            log "WARNING: Failed to get assessment progress"
            sleep $CHECK_INTERVAL
            elapsed_time=$((elapsed_time + CHECK_INTERVAL))
            continue
        fi
        
        local status
        status=$(echo "$progress_response" | jq -r '.status' 2>/dev/null || echo "ERROR")
        
        case "$status" in
            "READY")
                log "Assessment completed successfully"
                echo "$progress_response"
                return 0
                ;;
            "IN_PROGRESS")
                local progress
                progress=$(echo "$progress_response" | jq -r '.endpoints[0].progress // "0"' 2>/dev/null || echo "0")
                log "Assessment in progress: $progress% complete"
                ;;
            "DNS")
                log "Assessment status: Resolving DNS"
                ;;
            "ERROR")
                local errors
                errors=$(echo "$progress_response" | jq -r '.errors[]?.message' 2>/dev/null | tr '\n' ' ' || echo "Unknown error")
                log "ERROR: Assessment failed: $errors"
                return 1
                ;;
            *)
                log "Assessment status: $status"
                ;;
        esac
        
        sleep $CHECK_INTERVAL
        elapsed_time=$((elapsed_time + CHECK_INTERVAL))
    done
    
    log "ERROR: Assessment timed out after ${MAX_WAIT_TIME} seconds"
    return 1
}

# Parse assessment results
parse_assessment_results() {
    local assessment_data=$1
    
    log "Parsing assessment results"
    
    # Check if we have valid data
    if [ -z "$assessment_data" ] || ! echo "$assessment_data" | jq . >/dev/null 2>&1; then
        log "ERROR: Invalid assessment data received"
        return 1
    fi
    
    # Extract endpoint information
    local endpoints_count
    endpoints_count=$(echo "$assessment_data" | jq '.endpoints | length' 2>/dev/null || echo "0")
    
    if [ "$endpoints_count" -eq 0 ]; then
        log "ERROR: No endpoints found in assessment results"
        return 1
    fi
    
    log "Found $endpoints_count endpoint(s) for assessment"
    
    # Analyze each endpoint
    local overall_grade=""
    local lowest_score=100
    
    for ((i=0; i<endpoints_count; i++)); do
        local endpoint_ip endpoint_grade endpoint_details
        
        endpoint_ip=$(echo "$assessment_data" | jq -r ".endpoints[$i].ipAddress" 2>/dev/null || echo "Unknown")
        endpoint_grade=$(echo "$assessment_data" | jq -r ".endpoints[$i].grade" 2>/dev/null || echo "Unknown")
        
        log "Endpoint $endpoint_ip: Grade $endpoint_grade"
        
        # Get detailed information
        endpoint_details=$(echo "$assessment_data" | jq ".endpoints[$i].details" 2>/dev/null || echo "{}")
        
        # Extract key security metrics
        if [ "$endpoint_details" != "{}" ]; then
            local cert_score protocol_score key_exchange_score cipher_score
            
            cert_score=$(echo "$endpoint_details" | jq -r '.cert.score // "N/A"' 2>/dev/null)
            protocol_score=$(echo "$endpoint_details" | jq -r '.protocols.score // "N/A"' 2>/dev/null)
            key_exchange_score=$(echo "$endpoint_details" | jq -r '.keyExchange.score // "N/A"' 2>/dev/null)
            cipher_score=$(echo "$endpoint_details" | jq -r '.cipher.score // "N/A"' 2>/dev/null)
            
            log "  Certificate score: $cert_score"
            log "  Protocol score: $protocol_score"
            log "  Key exchange score: $key_exchange_score"
            log "  Cipher strength score: $cipher_score"
            
            # Check for vulnerabilities
            local vulnerabilities
            vulnerabilities=$(echo "$endpoint_details" | jq -r '.vulns | keys[]' 2>/dev/null | tr '\n' ' ' || echo "")
            
            if [ -n "$vulnerabilities" ]; then
                log "  Vulnerabilities detected: $vulnerabilities"
            else
                log "  No known vulnerabilities detected"
            fi
        fi
        
        # Track overall grade (use lowest grade from all endpoints)
        if [ -n "${GRADE_SCORES[$endpoint_grade]:-}" ]; then
            local current_score=${GRADE_SCORES[$endpoint_grade]}
            if [ "$current_score" -lt "$lowest_score" ]; then
                lowest_score=$current_score
                overall_grade=$endpoint_grade
            fi
        fi
    done
    
    # Store results globally
    OVERALL_GRADE="$overall_grade"
    ASSESSMENT_DATA="$assessment_data"
    
    return 0
}

# Generate grade report
generate_grade_report() {
    log "Generating grade report"
    
    local timestamp
    timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    # Determine pass/fail status
    local min_score=${GRADE_SCORES[$MIN_GRADE]:-90}
    local actual_score=${GRADE_SCORES[$OVERALL_GRADE]:-0}
    local status
    
    if [ "$actual_score" -ge "$min_score" ]; then
        status="PASS"
    else
        status="FAIL"
    fi
    
    # Create detailed report
    cat > "$REPORT_FILE" << EOF
{
    "domain": "$DOMAIN",
    "timestamp": "$timestamp",
    "assessment_summary": {
        "overall_grade": "$OVERALL_GRADE",
        "required_minimum": "$MIN_GRADE",
        "status": "$status",
        "grade_score": $actual_score,
        "minimum_score": $min_score
    },
    "raw_assessment": $ASSESSMENT_DATA
}
EOF
    
    log "Grade report saved to $REPORT_FILE"
    
    # Generate human-readable summary
    cat > "${REPORT_FILE%.json}.txt" << EOF
SSL Labs Grade Report for $DOMAIN
Generated: $timestamp

Overall Grade: $OVERALL_GRADE
Required Minimum: $MIN_GRADE
Status: $status

Grade Details:
- Grade Score: $actual_score/100
- Minimum Required: $min_score/100

$([ "$status" == "PASS" ] && echo "✓ SSL configuration meets or exceeds minimum requirements" || echo "✗ SSL configuration does not meet minimum requirements")

For detailed technical analysis, see: $REPORT_FILE
SSL Labs Public Report: https://www.ssllabs.com/ssltest/analyze.html?d=$DOMAIN
EOF
    
    log "Human-readable report saved to ${REPORT_FILE%.json}.txt"
}

# Check grade and send alerts
check_grade_compliance() {
    log "Checking grade compliance"
    
    local min_score=${GRADE_SCORES[$MIN_GRADE]:-90}
    local actual_score=${GRADE_SCORES[$OVERALL_GRADE]:-0}
    
    if [ "$actual_score" -ge "$min_score" ]; then
        log "SUCCESS: SSL grade ($OVERALL_GRADE) meets minimum requirement ($MIN_GRADE)"
        send_notification "INFO" "SSL Grade Check Passed" "Domain $DOMAIN achieved grade $OVERALL_GRADE (required: $MIN_GRADE)"
        return 0
    else
        log "FAILURE: SSL grade ($OVERALL_GRADE) below minimum requirement ($MIN_GRADE)"
        
        local detailed_message="SSL grade check failed for $DOMAIN
        
Current Grade: $OVERALL_GRADE (score: $actual_score/100)
Required Minimum: $MIN_GRADE (score: $min_score/100)

Please review the SSL configuration and address any security issues.
Detailed report: $REPORT_FILE
SSL Labs Test: https://www.ssllabs.com/ssltest/analyze.html?d=$DOMAIN

Common issues that can affect SSL grade:
- Weak cipher suites
- Insecure protocol versions (SSL 3.0, TLS 1.0, TLS 1.1)
- Missing security headers
- Certificate chain issues
- Known vulnerabilities (Heartbleed, POODLE, etc.)

This automated check runs to ensure our SSL configuration maintains high security standards."
        
        send_notification "CRITICAL" "SSL Grade Below Minimum" "$detailed_message"
        return 1
    fi
}

# Main grade checking function
main() {
    log "=== SSL Labs Grade Check Started for $DOMAIN ==="
    
    # Check API availability first
    if ! check_api_availability; then
        log "SSL Labs API not available, skipping grade check"
        exit 1
    fi
    
    # Start new assessment
    if ! start_assessment; then
        log "Failed to start SSL assessment"
        exit 1
    fi
    
    # Wait for completion and get results
    local assessment_results
    if assessment_results=$(check_assessment_progress); then
        log "Assessment completed successfully"
    else
        log "Assessment failed or timed out"
        exit 1
    fi
    
    # Parse and analyze results
    if parse_assessment_results "$assessment_results"; then
        log "Results parsed successfully - Overall grade: $OVERALL_GRADE"
    else
        log "Failed to parse assessment results"
        exit 1
    fi
    
    # Generate reports
    generate_grade_report
    
    # Check compliance and send alerts
    if check_grade_compliance; then
        log "=== SSL Grade Check Completed Successfully ==="
        exit 0
    else
        log "=== SSL Grade Check Completed with Failures ==="
        exit 1
    fi
}

# Handle command line arguments
case "${1:-check}" in
    "check")
        main
        ;;
    "quick")
        # Quick check using existing cached results if available
        MIN_GRADE="${2:-$MIN_GRADE}"
        log "Quick SSL grade check (using cached results if available)"
        
        local cached_response
        cached_response=$(curl -s "$API_BASE/analyze?host=$DOMAIN&fromCache=on&all=done" 2>/dev/null || echo "")
        
        if [ -n "$cached_response" ] && echo "$cached_response" | jq -r '.status' 2>/dev/null | grep -q "READY"; then
            log "Using cached assessment results"
            if parse_assessment_results "$cached_response"; then
                generate_grade_report
                check_grade_compliance
            else
                log "Failed to parse cached results, running full assessment"
                main
            fi
        else
            log "No cached results available, running full assessment"
            main
        fi
        ;;
    "report-only")
        if [ -f "$REPORT_FILE" ]; then
            log "Displaying existing grade report"
            cat "${REPORT_FILE%.json}.txt"
        else
            log "No existing report found. Run a grade check first."
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 {check|quick|report-only} [minimum-grade]"
        echo "  check       - Full SSL Labs assessment (default)"
        echo "  quick       - Quick check using cached results if available"
        echo "  report-only - Display last report without new assessment"
        echo ""
        echo "Minimum grade options: A+, A, A-, B, C, D, E, F"
        echo "Current minimum: $MIN_GRADE"
        exit 1
        ;;
esac