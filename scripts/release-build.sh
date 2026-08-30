#!/bin/bash
# GlowUp AI - Release Build Script
# Usage: ./scripts/release-build.sh [staging|production]

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  GlowUp AI - Release Build Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check build type argument
BUILD_TYPE="${1:-production}"
if [[ "$BUILD_TYPE" != "staging" && "$BUILD_TYPE" != "production" ]]; then
    echo -e "${RED}Error: Invalid build type '$BUILD_TYPE'${NC}"
    echo "Usage: $0 [staging|production]"
    exit 1
fi

echo -e "${GREEN}Build type: $BUILD_TYPE${NC}"
echo ""

# Navigate to project root
cd "$PROJECT_ROOT"

# ============================================
# Step 1: Validate Prerequisites
# ============================================
echo -e "${YELLOW}[1/7] Validating prerequisites...${NC}"

# Check keystore.properties exists
if [[ ! -f "app/keystore.properties" ]]; then
    echo -e "${RED}Error: app/keystore.properties not found${NC}"
    echo "Please create it from app/keystore.properties.example"
    echo "See RELEASE_BUILD_GUIDE.md Step 2 for instructions"
    exit 1
fi
echo "  ✓ keystore.properties found"

# Check google-services.json exists
if [[ ! -f "app/google-services.json" ]]; then
    echo -e "${RED}Error: app/google-services.json not found${NC}"
    echo "Please download it from Firebase Console"
    echo "See RELEASE_BUILD_GUIDE.md Step 4 for instructions"
    exit 1
fi
echo "  ✓ google-services.json found"

# Check gradlew exists
if [[ ! -f "gradlew" ]]; then
    echo -e "${RED}Error: gradlew not found${NC}"
    exit 1
fi
echo "  ✓ gradlew found"

echo -e "${GREEN}All prerequisites validated!${NC}"
echo ""

# ============================================
# Step 2: Get API URL
# ============================================
echo -e "${YELLOW}[2/7] Configuring API URL...${NC}"

if [[ "$BUILD_TYPE" == "staging" ]]; then
    # Staging build
    if [[ -z "${STAGING_API_BASE_URL}" ]]; then
        echo -e "${BLUE}Enter staging API URL (e.g., https://staging.glowup.example.com/api/):${NC}"
        read -r STAGING_API_BASE_URL

        if [[ -z "${STAGING_API_BASE_URL}" ]]; then
            echo -e "${RED}Error: Staging API URL is required${NC}"
            exit 1
        fi
    fi
    API_URL="$STAGING_API_BASE_URL"
    GRADLE_TASK="assembleStaging"
    GRADLE_BUNDLE_TASK="bundleStaging"
    GRADLE_PROPERTY="-PSTAGING_API_BASE_URL=$STAGING_API_BASE_URL"
    OUTPUT_DIR="staging"
else
    # Production build
    if [[ -z "${RELEASE_API_BASE_URL}" ]]; then
        echo -e "${BLUE}Enter production API URL (e.g., https://api.glowup.example.com/api/):${NC}"
        read -r RELEASE_API_BASE_URL

        if [[ -z "${RELEASE_API_BASE_URL}" ]]; then
            echo -e "${RED}Error: Production API URL is required${NC}"
            exit 1
        fi
    fi
    API_URL="$RELEASE_API_BASE_URL"
    GRADLE_TASK="assembleRelease"
    GRADLE_BUNDLE_TASK="bundleRelease"
    GRADLE_PROPERTY="-PRELEASE_API_BASE_URL=$RELEASE_API_BASE_URL"
    OUTPUT_DIR="release"
fi

echo -e "${GREEN}API URL: $API_URL${NC}"
echo ""

# ============================================
# Step 3: Read Current Version
# ============================================
echo -e "${YELLOW}[3/7] Reading current version...${NC}"

VERSION_CODE=$(grep 'versionCode' app/build.gradle.kts | sed 's/.*= *//' | tr -d ' ')
VERSION_NAME=$(grep 'versionName' app/build.gradle.kts | sed 's/.*= *//' | tr -d ' "')

echo "  Version Code: $VERSION_CODE"
echo "  Version Name: $VERSION_NAME"
echo ""

# ============================================
# Step 4: Clean Previous Build
# ============================================
echo -e "${YELLOW}[4/7] Cleaning previous build...${NC}"

./gradlew clean > /dev/null 2>&1 || {
    echo -e "${RED}Clean failed${NC}"
    exit 1
}

echo -e "${GREEN}Clean completed!${NC}"
echo ""

# ============================================
# Step 5: Build Release AAB (App Bundle)
# ============================================
echo -e "${YELLOW}[5/7] Building release App Bundle...${NC}"
echo "This may take a few minutes..."
echo ""

./gradlew ":app:$GRADLE_BUNDLE_TASK" "$GRADLE_PROPERTY" || {
    echo ""
    echo -e "${RED}Build failed!${NC}"
    echo "Check the error messages above"
    exit 1
}

echo ""
echo -e "${GREEN}Build completed successfully!${NC}"
echo ""

# ============================================
# Step 6: Verify Signing
# ============================================
echo -e "${YELLOW}[6/7] Verifying release signing...${NC}"

AAB_PATH="app/build/outputs/bundle/$OUTPUT_DIR/app-$OUTPUT_DIR.aab"

if [[ ! -f "$AAB_PATH" ]]; then
    echo -e "${RED}Error: App Bundle not found at $AAB_PATH${NC}"
    exit 1
fi

# Verify AAB is signed
if jarsigner -verify "$AAB_PATH" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ App Bundle is properly signed${NC}"

    # Show certificate details
    echo ""
    echo "Certificate details:"
    jarsigner -verify -verbose -certs "$AAB_PATH" 2>&1 | grep -A 3 "Signed by" || echo "  (Details available via jarsigner -verify -verbose -certs)"
else
    echo -e "${RED}Warning: App Bundle signature verification failed${NC}"
    echo "This might indicate debug signing - DO NOT upload to Play Store!"
fi

echo ""

# ============================================
# Step 7: Build Summary
# ============================================
echo -e "${YELLOW}[7/7] Build Summary${NC}"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       BUILD SUCCESSFUL!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Build Type:    $BUILD_TYPE"
echo "Version:       $VERSION_NAME (code: $VERSION_CODE)"
echo "API URL:       $API_URL"
echo ""
echo "Output Files:"
echo ""

# Show AAB info
if [[ -f "$AAB_PATH" ]]; then
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    echo "  📦 App Bundle (AAB) - UPLOAD THIS TO PLAY STORE"
    echo "     Location: $AAB_PATH"
    echo "     Size:     $AAB_SIZE"
    echo ""
fi

# Show APK info (if exists - staging/release might generate APK too)
APK_PATH="app/build/outputs/apk/$OUTPUT_DIR/app-$OUTPUT_DIR.apk"
if [[ -f "$APK_PATH" ]]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "  📱 APK - For testing only"
    echo "     Location: $APK_PATH"
    echo "     Size:     $APK_SIZE"
    echo ""
fi

# ============================================
# Next Steps
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}       NEXT STEPS${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [[ "$BUILD_TYPE" == "staging" ]]; then
    echo "1. Test the build on a physical device:"
    echo "   adb install $APK_PATH"
    echo ""
    echo "2. Test these critical flows:"
    echo "   - Firebase Authentication (Email + Google)"
    echo "   - Network connectivity to staging backend"
    echo "   - Camera capture and upload"
    echo "   - All navigation and key features"
    echo ""
    echo "3. Once validated, build for production:"
    echo "   ./scripts/release-build.sh production"
else
    echo "1. Test the build on a physical device FIRST:"
    if [[ -f "$APK_PATH" ]]; then
        echo "   adb install $APK_PATH"
    else
        echo "   Build APK first: ./gradlew :app:assembleRelease $GRADLE_PROPERTY"
    fi
    echo ""
    echo "2. Verify these before uploading to Play Store:"
    echo "   ✓ Firebase Auth works (Email + Google Sign-In)"
    echo "   ✓ App reaches production backend successfully"
    echo "   ✓ Camera and photo upload work"
    echo "   ✓ No crashes or critical bugs"
    echo "   ✓ All key features work as expected"
    echo ""
    echo "3. Prepare Play Store assets:"
    echo "   ✓ Screenshots (phone, tablet)"
    echo "   ✓ Feature graphic (1024x500)"
    echo "   ✓ Release notes for this version"
    echo "   ✓ Privacy Policy URL"
    echo "   ✓ Data Safety form completed"
    echo ""
    echo "4. Upload to Google Play Console:"
    echo "   https://play.google.com/console"
    echo ""
    echo "   Navigate to: Release → Production (or Internal testing)"
    echo "   Upload: $AAB_PATH"
    echo ""
    echo "5. For next release, bump version:"
    echo "   ./scripts/version-bump.sh [new-version-name] [new-version-code]"
fi

echo ""
echo -e "${GREEN}Done! 🚀${NC}"
echo ""
