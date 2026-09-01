#!/bin/bash
# Railway Deployment Helper Commands
# Usage: Source this file or run individual commands

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# SETUP COMMANDS
# ============================================================================

# Install Railway CLI
install_railway_cli() {
    echo_info "Installing Railway CLI..."
    npm install -g @railway/cli
    railway --version
}

# Login to Railway
railway_login() {
    echo_info "Logging into Railway..."
    railway login
}

# Initialize Railway project
railway_init() {
    echo_info "Initializing Railway project..."
    cd /Users/21cabbage/Skinproof/backend
    railway init
}

# Link existing project
railway_link() {
    echo_info "Available projects:"
    railway list
    echo_info "Enter project ID to link:"
    read project_id
    railway link $project_id
}

# ============================================================================
# SECRET GENERATION
# ============================================================================

# Generate admin token
generate_admin_token() {
    echo_info "Generating admin token..."
    python3 -c "import secrets; print(secrets.token_urlsafe(32))"
}

# Generate photo encryption key
generate_photo_key() {
    echo_info "Generating photo encryption key (base64)..."
    python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
}

# Generate all secrets at once
generate_all_secrets() {
    echo_info "Generating all required secrets..."
    echo ""
    echo "GLOWUPAI_ADMIN_TOKEN:"
    generate_admin_token
    echo ""
    echo "GLOWUPAI_PHOTO_KEY:"
    generate_photo_key
    echo ""
    echo_info "Copy these values to Railway Dashboard -> Variables"
}

# ============================================================================
# DEPLOYMENT COMMANDS
# ============================================================================

# Deploy to Railway
railway_deploy() {
    echo_info "Deploying to Railway..."
    cd /Users/21cabbage/Skinproof/backend
    railway up
}

# Check deployment status
railway_status() {
    echo_info "Checking Railway status..."
    railway status
}

# View recent deployments
railway_list_deployments() {
    echo_info "Recent deployments:"
    railway list
}

# ============================================================================
# MONITORING COMMANDS
# ============================================================================

# View logs (live)
railway_logs() {
    echo_info "Streaming Railway logs (Ctrl+C to exit)..."
    railway logs
}

# View logs (last 100 lines)
railway_logs_recent() {
    echo_info "Recent logs (last 100 lines)..."
    railway logs --limit 100
}

# Check service health
check_health() {
    echo_info "Getting Railway URL..."
    url=$(railway domain)
    if [ -z "$url" ]; then
        echo_error "No Railway domain found. Deploy first!"
        return 1
    fi

    echo_info "Checking health at: https://$url/api/health"
    response=$(curl -s "https://$url/api/health")
    echo "$response" | python3 -m json.tool

    if echo "$response" | grep -q '"status": "ok"'; then
        echo_info "✓ Service is healthy!"
    else
        echo_error "✗ Service health check failed!"
        return 1
    fi
}

# ============================================================================
# DATABASE COMMANDS
# ============================================================================

# Check database connection
check_database() {
    echo_info "Checking database connection..."
    railway run python3 -c "
from glowupai.config import Settings
from glowupai.complete_db import build_full_database

settings = Settings.from_env()
db = build_full_database(settings)
result = db.healthcheck()
print(f'Database healthy: {result}')
"
}

# Run database migrations manually
run_migrations() {
    echo_info "Running database migrations..."
    railway run python3 -c "
from glowupai.config import Settings
from glowupai.complete_db import build_full_database

settings = Settings.from_env()
db = build_full_database(settings)
print('Migrations completed successfully')
"
}

# ============================================================================
# ENVIRONMENT VARIABLE MANAGEMENT
# ============================================================================

# Show current environment variables
show_vars() {
    echo_info "Current Railway environment variables:"
    railway vars
}

# Set environment variable
set_var() {
    if [ -z "$1" ] || [ -z "$2" ]; then
        echo_error "Usage: set_var KEY VALUE"
        return 1
    fi
    echo_info "Setting $1..."
    railway vars set "$1=$2"
}

# Delete environment variable
delete_var() {
    if [ -z "$1" ]; then
        echo_error "Usage: delete_var KEY"
        return 1
    fi
    echo_warn "Deleting $1..."
    railway vars delete "$1"
}

# Load variables from .env.production
load_production_vars() {
    if [ ! -f ".env.production" ]; then
        echo_error ".env.production file not found!"
        echo_info "Create it from .env.production.template first"
        return 1
    fi

    echo_warn "This will set all variables from .env.production"
    echo_warn "Make sure secrets are filled in!"
    echo "Continue? (y/N)"
    read confirm

    if [ "$confirm" != "y" ]; then
        echo_info "Cancelled"
        return 0
    fi

    echo_info "Loading variables..."
    railway vars set $(cat .env.production | grep -v '^#' | grep -v '^$' | tr '\n' ' ')
    echo_info "Variables loaded. Service will restart automatically."
}

# ============================================================================
# TESTING COMMANDS
# ============================================================================

# Test API endpoint
test_endpoint() {
    if [ -z "$1" ]; then
        echo_error "Usage: test_endpoint /api/endpoint"
        return 1
    fi

    url=$(railway domain)
    if [ -z "$url" ]; then
        echo_error "No Railway domain found!"
        return 1
    fi

    echo_info "Testing: https://$url$1"
    curl -v "https://$url$1"
}

# Test user creation
test_create_user() {
    url=$(railway domain)
    if [ -z "$url" ]; then
        echo_error "No Railway domain found!"
        return 1
    fi

    echo_info "Creating test user..."
    curl -X POST "https://$url/api/users" \
        -H "Content-Type: application/json" \
        -d '{"skin_type": "combination"}' \
        | python3 -m json.tool
}

# ============================================================================
# ROLLBACK COMMANDS
# ============================================================================

# Rollback to previous deployment
railway_rollback() {
    echo_warn "Rolling back to previous deployment..."
    railway rollback
}

# Rollback to specific deployment
railway_rollback_to() {
    if [ -z "$1" ]; then
        echo_error "Usage: railway_rollback_to <deployment-id>"
        echo_info "Get deployment ID from: railway list"
        return 1
    fi
    echo_warn "Rolling back to deployment: $1"
    railway rollback "$1"
}

# ============================================================================
# MAINTENANCE COMMANDS
# ============================================================================

# Restart service
railway_restart() {
    echo_info "Restarting Railway service..."
    railway restart
}

# Open Railway dashboard
railway_open() {
    echo_info "Opening Railway dashboard in browser..."
    railway open
}

# Get Railway domain
railway_domain() {
    echo_info "Railway domain:"
    railway domain
}

# ============================================================================
# HELP MENU
# ============================================================================

show_help() {
    cat << EOF
${GREEN}Railway Deployment Helper Commands${NC}

${YELLOW}Setup:${NC}
  install_railway_cli     - Install Railway CLI
  railway_login          - Login to Railway
  railway_init           - Initialize new project
  railway_link           - Link existing project

${YELLOW}Secrets:${NC}
  generate_admin_token   - Generate GLOWUPAI_ADMIN_TOKEN
  generate_photo_key     - Generate GLOWUPAI_PHOTO_KEY
  generate_all_secrets   - Generate all required secrets

${YELLOW}Deployment:${NC}
  railway_deploy         - Deploy to Railway
  railway_status         - Check deployment status
  railway_list_deployments - List recent deployments

${YELLOW}Monitoring:${NC}
  railway_logs           - Stream live logs
  railway_logs_recent    - View last 100 log lines
  check_health          - Check API health endpoint
  check_database        - Test database connection

${YELLOW}Environment Variables:${NC}
  show_vars             - Show current variables
  set_var KEY VALUE     - Set a variable
  delete_var KEY        - Delete a variable
  load_production_vars  - Load from .env.production

${YELLOW}Testing:${NC}
  test_endpoint /api/path - Test API endpoint
  test_create_user       - Test user creation

${YELLOW}Maintenance:${NC}
  railway_restart       - Restart service
  railway_rollback      - Rollback to previous deployment
  railway_open          - Open dashboard in browser
  railway_domain        - Show Railway domain

${YELLOW}Quick Start:${NC}
  1. install_railway_cli
  2. railway_login
  3. railway_init
  4. generate_all_secrets
  5. Set variables in Railway dashboard
  6. railway_deploy
  7. check_health

EOF
}

# ============================================================================
# MAIN
# ============================================================================

# If script is run directly, show help
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    show_help
    echo ""
    echo_info "Source this file to use the functions:"
    echo_info "  source railway-commands.sh"
    echo_info "  show_help"
fi
