#!/bin/bash
# Flash Sale System - Transaction and Consistency Test
# One-click execution of complete business flow test

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check service health
check_health() {
    log_info "Checking service health..."

    if ! curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1 && \
       ! curl -s "${BASE_URL}/api/users/info" > /dev/null 2>&1; then
        log_error "Service is not responding at ${BASE_URL}"
        exit 1
    fi

    log_info "Service is healthy"
}

# Test 1: Query flash sale activities
test_query_activities() {
    log_info "Step 1: Querying flash sale activities..."

    RESPONSE=$(curl -s "${BASE_URL}/api/seckill/activities")

    if echo "$RESPONSE" | grep -q '"id"'; then
        log_info "Activities retrieved successfully"
        echo "$RESPONSE"
    else
        log_error "Failed to query activities: $RESPONSE"
        exit 1
    fi
}

# Test 2: Query active activities
test_active_activities() {
    log_info "Step 2: Querying active activities..."

    RESPONSE=$(curl -s "${BASE_URL}/api/seckill/activities/active")

    if echo "$RESPONSE" | grep -q '"id"'; then
        log_info "Active activities retrieved successfully"
    else
        log_warn "No active activities found (this is OK if none are scheduled)"
    fi
}

# Test 3: Initialize flash sale stock
test_init_stock() {
    local activity_id=${1:-1}
    local product_id=${2:-1}

    log_info "Step 3: Initializing flash sale stock (activity=${activity_id}, product=${product_id})..."

    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/seckill/activities/${activity_id}/products/${product_id}/init-stock")

    if echo "$RESPONSE" | grep -q '"success":true'; then
        log_info "Stock initialized successfully"
    else
        log_error "Failed to initialize stock: $RESPONSE"
        exit 1
    fi
}

# Test 4: Flash sale order
test_seckill_order() {
    local user_id=${1:-$((RANDOM % 10000 + 100))}
    local activity_id=${2:-1}
    local product_id=${3:-1}

    log_info "Step 4: Flash sale order (userId=${user_id}, activityId=${activity_id}, productId=${product_id})..."

    RESPONSE=$(curl -s -X POST \
        "${BASE_URL}/api/seckill/order?userId=${user_id}&activityId=${activity_id}&productId=${product_id}")

    echo "$RESPONSE" | grep -q '"success":true' && ORDER_RESULT="success" || ORDER_RESULT="fail"
    ORDER_MSG=$(echo "$RESPONSE" | grep -o '"message":"[^"]*"' | head -1 | cut -d'"' -f4)
    ORDER_NO=$(echo "$RESPONSE" | grep -o '"orderNo":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$ORDER_RESULT" = "success" ]; then
        log_info "Flash sale order succeeded, orderNo: ${ORDER_NO:-N/A}, message: $ORDER_MSG"
        echo "$ORDER_NO"
    else
        log_warn "Flash sale order failed, message: $ORDER_MSG"
        echo ""
    fi
}

# Test 5: Query order
test_query_order() {
    local order_no=$1

    if [ -z "$order_no" ]; then
        log_warn "No order number provided, skipping order query"
        return
    fi

    log_info "Step 5: Querying order ${order_no}..."

    RESPONSE=$(curl -s "${BASE_URL}/api/seckill/order/${order_no}")

    if echo "$RESPONSE" | grep -q '"orderNo"'; then
        log_info "Order found: $RESPONSE"
    else
        log_error "Order not found: $RESPONSE"
        exit 1
    fi
}

# Test 6: Query user orders
test_user_orders() {
    local user_id=${1:-$((RANDOM % 10000 + 100))}

    log_info "Step 6: Querying orders for user ${user_id}..."

    RESPONSE=$(curl -s "${BASE_URL}/api/seckill/orders/user/${user_id}")

    log_info "User orders retrieved"
}

# Test 7: Product detail (cache test)
test_product_detail() {
    local product_id=${1:-1}

    log_info "Step 7: Testing product detail cache (productId=${product_id})..."

    for i in 1 2 3; do
        RESPONSE=$(curl -s -w "\n%{time_total}" "${BASE_URL}/api/products/${product_id}")
        TIME=$(echo "$RESPONSE" | tail -1)
        BODY=$(echo "$RESPONSE" | head -1)
        if echo "$BODY" | grep -q '"id"'; then
            log_info "  Request ${i}: OK, time=${TIME}s"
        else
            log_warn "  Request ${i}: Product not found (this is OK if no data exists)"
        fi
    done
}

# Test 8: Concurrent flash sale (basic concurrency test)
test_concurrent_seckill() {
    log_info "Step 8: Testing concurrent flash sale (10 parallel requests)..."

    local pids=()
    local success_count=0
    local fail_count=0

    for i in $(seq 1 10); do
        user_id=$((RANDOM % 1000 + 10000))
        (
            RESPONSE=$(curl -s -X POST \
                "${BASE_URL}/api/seckill/order?userId=${user_id}&activityId=1&productId=1")
            if echo "$RESPONSE" | grep -q '"success":true'; then
                echo "SUCCESS"
            else
                echo "FAIL"
            fi
        ) &
        pids+=($!)
    done

    for pid in "${pids[@]}"; do
        result=$(wait $pid)
        if [ "$result" = "SUCCESS" ]; then
            ((success_count++))
        else
            ((fail_count++))
        fi
    done

    log_info "Concurrent test results: ${success_count} succeeded, ${fail_count} failed"
}

# Test 9: Duplicate order prevention (per-user limit)
test_duplicate_order() {
    local user_id=${1:-$((RANDOM % 10000 + 200))}
    local activity_id=${2:-1}
    local product_id=${3:-1}

    log_info "Step 9: Testing duplicate order prevention (userId=${user_id})..."

    # First order
    RESPONSE1=$(curl -s -X POST \
        "${BASE_URL}/api/seckill/order?userId=${user_id}&activityId=${activity_id}&productId=${product_id}")

    # Second order (should be blocked or handled gracefully)
    RESPONSE2=$(curl -s -X POST \
        "${BASE_URL}/api/seckill/order?userId=${user_id}&activityId=${activity_id}&productId=${product_id}")

    if echo "$RESPONSE2" | grep -q '"message"'; then
        log_info "Duplicate order handled: $(echo "$RESPONSE2" | grep -o '"message":"[^"]*"' | head -1 | cut -d'"' -f4)"
    fi
}

# Cleanup
cleanup() {
    log_info "Cleaning up test data..."

    # Flush Redis (if Redis is accessible)
    docker exec flash-sale-redis redis-cli -a redis123 FLUSHDB > /dev/null 2>&1 || \
    docker exec redis redis-cli FLUSHDB > /dev/null 2>&1 || true

    log_info "Cleanup complete"
}

# Usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --skip-stock       Skip stock initialization"
    echo "  --skip-concurrent  Skip concurrent test"
    echo "  --skip-duplicate   Skip duplicate order test"
    echo "  --cleanup          Run cleanup after tests"
    echo "  --url URL          Set base URL (default: http://localhost:8080)"
    echo "  -h, --help        Show this help"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests"
    echo "  $0 --skip-concurrent                  # Skip concurrent test"
    echo "  $0 --url http://localhost:80          # Test via Nginx"
}

# Parse arguments
SKIP_STOCK=false
SKIP_CONCURRENT=false
SKIP_DUPLICATE=false
RUN_CLEANUP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-stock) SKIP_STOCK=true; shift ;;
        --skip-concurrent) SKIP_CONCURRENT=true; shift ;;
        --skip-duplicate) SKIP_DUPLICATE=true; shift ;;
        --cleanup) RUN_CLEANUP=true; shift ;;
        --url) BASE_URL="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1"; usage; exit 1 ;;
    esac
done

# Main flow
main() {
    echo "========================================"
    echo "  Flash Sale System Test Suite"
    echo "  Base URL: ${BASE_URL}"
    echo "========================================"
    echo ""

    # Check service
    check_health

    # Query activities
    test_query_activities

    # Query active activities
    test_active_activities

    # Product detail cache test
    test_product_detail 1

    # Initialize stock
    if [ "$SKIP_STOCK" = "false" ]; then
        test_init_stock 1 1
    else
        log_info "Skipping stock initialization"
    fi

    # Flash sale order
    ORDER_NO=$(test_seckill_order 100 1 1)
    test_query_order "$ORDER_NO"
    test_user_orders 100

    # Concurrent flash sale
    if [ "$SKIP_CONCURRENT" = "false" ]; then
        test_concurrent_seckill
    else
        log_info "Skipping concurrent test"
    fi

    # Duplicate order prevention
    if [ "$SKIP_DUPLICATE" = "false" ]; then
        test_duplicate_order 999 1 1
    else
        log_info "Skipping duplicate order test"
    fi

    echo ""
    echo "========================================"
    log_info "All tests completed!"
    echo "========================================"

    # Optional cleanup
    if [ "$RUN_CLEANUP" = "true" ]; then
        cleanup
    fi
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi
