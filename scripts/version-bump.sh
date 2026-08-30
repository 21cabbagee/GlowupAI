#!/bin/bash
# GlowUp AI - Version Bump Script
# Usage: ./scripts/version-bump.sh <version-name> <version-code>
# Example: ./scripts/version-bump.sh 1.1.0 2

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
echo -e "${BLUE}  GlowUp AI - Version Bump Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ============================================
# Parse Arguments
# ============================================
NEW_VERSION_NAME="$1"
NEW_VERSION_CODE="$2"

if [[ -z "$NEW_VERSION_NAME" || -z "$NEW_VERSION_CODE" ]]; then
    echo -e "${RED}Error: Missing arguments${NC}"
    echo ""
    echo "Usage: $0 <version-name> <version-code>"
    echo ""
    echo "Examples:"
    echo "  $0 1.1.0 2      # Bump to version 1.1.0 (code 2)"
    echo "  $0 2.0.0 10     # Bump to version 2.0.0 (code 10)"
    echo ""
    exit 1
fi

# Validate version code is a number
if ! [[ "$NEW_VERSION_CODE" =~ ^[0-9]+$ ]]; then
    echo -e "${RED}Error: Version code must be a number${NC}"
    echo "Got: $NEW_VERSION_CODE"
    exit 1
fi

# Navigate to project root
cd "$PROJECT_ROOT"

# ============================================
# Read Current Version
# ============================================
echo -e "${YELLOW}Reading current version...${NC}"

BUILD_GRADLE="app/build.gradle.kts"

if [[ ! -f "$BUILD_GRADLE" ]]; then
    echo -e "${RED}Error: $BUILD_GRADLE not found${NC}"
    exit 1
fi

CURRENT_VERSION_CODE=$(grep 'versionCode' "$BUILD_GRADLE" | sed 's/.*= *//' | tr -d ' ')
CURRENT_VERSION_NAME=$(grep 'versionName' "$BUILD_GRADLE" | sed 's/.*= *//' | tr -d ' "')

echo "  Current: $CURRENT_VERSION_NAME (code: $CURRENT_VERSION_CODE)"
echo "  New:     $NEW_VERSION_NAME (code: $NEW_VERSION_CODE)"
echo ""

# ============================================
# Validation
# ============================================
echo -e "${YELLOW}Validating version bump...${NC}"

# Check version code is incrementing
if [[ "$NEW_VERSION_CODE" -le "$CURRENT_VERSION_CODE" ]]; then
    echo -e "${RED}Error: New version code must be greater than current${NC}"
    echo "Current version code: $CURRENT_VERSION_CODE"
    echo "New version code: $NEW_VERSION_CODE"
    echo ""
    echo "Version code must increment for every Play Store release!"
    exit 1
fi

echo "  ✓ Version code incremented"

# Warn if version name is same (valid but unusual)
if [[ "$NEW_VERSION_NAME" == "$CURRENT_VERSION_NAME" ]]; then
    echo -e "${YELLOW}Warning: Version name is unchanged${NC}"
    echo "This is valid but unusual. Continue? (y/n)"
    read -r confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "Aborted"
        exit 0
    fi
fi

echo ""

# ============================================
# Confirm Changes
# ============================================
echo -e "${BLUE}Ready to bump version:${NC}"
echo ""
echo "  From: $CURRENT_VERSION_NAME (code: $CURRENT_VERSION_CODE)"
echo "  To:   $NEW_VERSION_NAME (code: $NEW_VERSION_CODE)"
echo ""
echo "This will modify $BUILD_GRADLE"
echo ""
echo -e "${YELLOW}Proceed? (y/n)${NC}"
read -r confirm

if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted"
    exit 0
fi

echo ""

# ============================================
# Update Version
# ============================================
echo -e "${YELLOW}Updating version in $BUILD_GRADLE...${NC}"

# Create backup
BACKUP_FILE="${BUILD_GRADLE}.backup"
cp "$BUILD_GRADLE" "$BACKUP_FILE"
echo "  ✓ Created backup: $BACKUP_FILE"

# Update versionCode
if grep -q "versionCode = $CURRENT_VERSION_CODE" "$BUILD_GRADLE"; then
    sed -i '' "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" "$BUILD_GRADLE"
    echo "  ✓ Updated versionCode: $CURRENT_VERSION_CODE → $NEW_VERSION_CODE"
else
    echo -e "${RED}Error: Could not find 'versionCode = $CURRENT_VERSION_CODE' in $BUILD_GRADLE${NC}"
    echo "Restoring backup..."
    mv "$BACKUP_FILE" "$BUILD_GRADLE"
    exit 1
fi

# Update versionName
if grep -q "versionName = \"$CURRENT_VERSION_NAME\"" "$BUILD_GRADLE"; then
    sed -i '' "s/versionName = \"$CURRENT_VERSION_NAME\"/versionName = \"$NEW_VERSION_NAME\"/" "$BUILD_GRADLE"
    echo "  ✓ Updated versionName: $CURRENT_VERSION_NAME → $NEW_VERSION_NAME"
else
    echo -e "${RED}Error: Could not find 'versionName = \"$CURRENT_VERSION_NAME\"' in $BUILD_GRADLE${NC}"
    echo "Restoring backup..."
    mv "$BACKUP_FILE" "$BUILD_GRADLE"
    exit 1
fi

echo ""

# ============================================
# Verify Changes
# ============================================
echo -e "${YELLOW}Verifying changes...${NC}"

UPDATED_VERSION_CODE=$(grep 'versionCode' "$BUILD_GRADLE" | sed 's/.*= *//' | tr -d ' ')
UPDATED_VERSION_NAME=$(grep 'versionName' "$BUILD_GRADLE" | sed 's/.*= *//' | tr -d ' "')

if [[ "$UPDATED_VERSION_CODE" != "$NEW_VERSION_CODE" ]]; then
    echo -e "${RED}Error: Version code verification failed${NC}"
    echo "Expected: $NEW_VERSION_CODE, Got: $UPDATED_VERSION_CODE"
    echo "Restoring backup..."
    mv "$BACKUP_FILE" "$BUILD_GRADLE"
    exit 1
fi

if [[ "$UPDATED_VERSION_NAME" != "$NEW_VERSION_NAME" ]]; then
    echo -e "${RED}Error: Version name verification failed${NC}"
    echo "Expected: $NEW_VERSION_NAME, Got: $UPDATED_VERSION_NAME"
    echo "Restoring backup..."
    mv "$BACKUP_FILE" "$BUILD_GRADLE"
    exit 1
fi

echo "  ✓ Version code: $UPDATED_VERSION_CODE"
echo "  ✓ Version name: $UPDATED_VERSION_NAME"
echo ""

# Remove backup after successful verification
rm "$BACKUP_FILE"

# ============================================
# Show Diff
# ============================================
echo -e "${BLUE}Changes made:${NC}"
echo ""
grep -A 2 "versionCode\|versionName" "$BUILD_GRADLE" | head -4
echo ""

# ============================================
# Success Summary
# ============================================
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       VERSION BUMPED SUCCESSFULLY!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Updated: $BUILD_GRADLE"
echo ""
echo "  Version Code: $CURRENT_VERSION_CODE → $NEW_VERSION_CODE"
echo "  Version Name: $CURRENT_VERSION_NAME → $NEW_VERSION_NAME"
echo ""

# ============================================
# Next Steps
# ============================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}       NEXT STEPS${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "1. Review the changes:"
echo "   git diff $BUILD_GRADLE"
echo ""
echo "2. Update release notes for version $NEW_VERSION_NAME"
echo ""
echo "3. Commit the version bump:"
echo "   git add $BUILD_GRADLE"
echo "   git commit -m \"Bump version to $NEW_VERSION_NAME (code: $NEW_VERSION_CODE)\""
echo ""
echo "4. Build the new release:"
echo "   ./scripts/release-build.sh production"
echo ""
echo "5. After release, tag the commit:"
echo "   git tag -a v$NEW_VERSION_NAME -m \"Release version $NEW_VERSION_NAME\""
echo "   git push origin v$NEW_VERSION_NAME"
echo ""

echo -e "${GREEN}Done! 🎉${NC}"
echo ""
