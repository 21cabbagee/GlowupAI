#!/bin/bash
# Comprehensive test runner for GlowupAI
# Usage: ./run_tests.sh [backend|android|all|load]

set -e  # Exit on error

COLOR_RESET="\033[0m"
COLOR_GREEN="\033[0;32m"
COLOR_YELLOW="\033[0;33m"
COLOR_RED="\033[0;31m"
COLOR_BLUE="\033[0;34m"

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_RESET} $1"
}

log_warning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

run_backend_tests() {
    log_info "Running Backend Tests..."

    cd backend

    # Check if venv exists
    if [ ! -d "venv" ]; then
        log_warning "Virtual environment not found. Creating..."
        python3 -m venv venv
    fi

    # Activate venv
    source venv/bin/activate

    # Install dependencies
    log_info "Installing dependencies..."
    pip install -q -e ".[dev]" 2>&1 | grep -v "already satisfied" || true

    # Run tests with coverage
    log_info "Running pytest with coverage..."
    pytest tests/ \
        --cov=skinproof \
        --cov-report=html \
        --cov-report=term \
        --junit-xml=pytest-report.xml \
        -v

    # Check coverage
    coverage_percent=$(coverage report | grep TOTAL | awk '{print $4}' | sed 's/%//')
    if [ $(echo "$coverage_percent >= 70" | bc -l) -eq 1 ]; then
        log_success "Coverage: ${coverage_percent}% (Target: 70%+) ✓"
    else
        log_warning "Coverage: ${coverage_percent}% (Target: 70%+) - Needs improvement"
    fi

    # Run linting
    log_info "Running code quality checks..."

    # Black
    if black --check skinproof tests 2>/dev/null; then
        log_success "Black formatting ✓"
    else
        log_warning "Black formatting issues found. Run: black skinproof tests"
    fi

    # Mypy
    if mypy skinproof --ignore-missing-imports --no-strict-optional 2>/dev/null; then
        log_success "Mypy type checking ✓"
    else
        log_warning "Mypy type issues found"
    fi

    # Bandit
    if bandit -r skinproof -ll 2>/dev/null; then
        log_success "Bandit security scan ✓"
    else
        log_warning "Bandit found potential security issues"
    fi

    deactivate
    cd ..

    log_success "Backend tests completed!"
    log_info "Coverage report: backend/htmlcov/index.html"
}

run_android_tests() {
    log_info "Running Android Tests..."

    # Check if gradlew exists
    if [ ! -f "gradlew" ]; then
        log_error "gradlew not found. Are you in the project root?"
        exit 1
    fi

    # Make gradlew executable
    chmod +x gradlew

    # Run unit tests
    log_info "Running unit tests..."
    ./gradlew testDebugUnitTest --console=plain

    # Run lint
    log_info "Running lint checks..."
    ./gradlew lintDebug --console=plain

    # Build debug APK
    log_info "Building debug APK..."
    ./gradlew assembleDebug --console=plain

    log_success "Android tests completed!"
    log_info "Test report: app/build/reports/tests/testDebugUnitTest/index.html"
    log_info "Lint report: app/build/reports/lint-results-debug.html"
}

run_android_ui_tests() {
    log_info "Running Android UI Tests (requires emulator)..."

    # Check if emulator is running
    if ! adb devices | grep -q "emulator"; then
        log_warning "No emulator detected. Starting emulator..."
        log_info "Please start an emulator first: emulator -avd YOUR_AVD_NAME"
        exit 1
    fi

    log_info "Running instrumented tests..."
    ./gradlew connectedDebugAndroidTest --console=plain

    log_success "UI tests completed!"
    log_info "Report: app/build/reports/androidTests/connected/index.html"
}

run_load_tests() {
    log_info "Running Load Tests..."
    log_warning "Ensure backend server is running at http://localhost:8000"

    cd backend
    source venv/bin/activate

    # Check if server is running
    if ! curl -s http://localhost:8000/api/roadmap > /dev/null; then
        log_error "Backend server not responding at http://localhost:8000"
        log_info "Start the server first: cd backend && uvicorn skinproof.api:app --reload"
        exit 1
    fi

    log_info "Starting Locust load tests..."
    log_info "Open http://localhost:8089 in your browser"
    log_info "Recommended: 100 users, 10 spawn rate, 5 minutes"

    locust -f tests/load/locustfile.py --host=http://localhost:8000

    deactivate
    cd ..
}

print_usage() {
    echo "Usage: ./run_tests.sh [backend|android|android-ui|load|all|quick]"
    echo ""
    echo "Commands:"
    echo "  backend     - Run backend tests (unit + integration)"
    echo "  android     - Run Android unit tests"
    echo "  android-ui  - Run Android UI tests (requires emulator)"
    echo "  load        - Run load tests (requires running server)"
    echo "  all         - Run all tests (backend + android)"
    echo "  quick       - Run quick tests only (no UI, no load)"
    echo ""
    echo "Examples:"
    echo "  ./run_tests.sh backend"
    echo "  ./run_tests.sh android"
    echo "  ./run_tests.sh all"
}

# Main script
main() {
    local command=${1:-all}

    log_info "GlowupAI Test Runner"
    log_info "===================="

    case $command in
        backend)
            run_backend_tests
            ;;
        android)
            run_android_tests
            ;;
        android-ui)
            run_android_ui_tests
            ;;
        load)
            run_load_tests
            ;;
        all)
            run_backend_tests
            echo ""
            run_android_tests
            ;;
        quick)
            log_info "Running quick tests (backend unit + android unit)..."
            run_backend_tests
            run_android_tests
            ;;
        help|--help|-h)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown command: $command"
            print_usage
            exit 1
            ;;
    esac

    echo ""
    log_success "✨ All tests completed successfully!"
}

# Run main function
main "$@"
