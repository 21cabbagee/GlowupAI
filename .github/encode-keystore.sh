#!/bin/bash
#
# GlowUp AI - Keystore Base64 Encoder
#
# This script helps you encode your release keystore to Base64 format
# for use in GitHub Actions secrets.
#
# Usage: ./encode-keystore.sh path/to/release.keystore
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================"
echo "  GlowUp AI - Keystore Base64 Encoder"
echo "================================================"
echo ""

# Check if keystore path is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: No keystore path provided${NC}"
    echo ""
    echo "Usage: $0 path/to/release.keystore"
    echo ""
    echo "Example:"
    echo "  $0 ../app/release.keystore"
    echo ""
    exit 1
fi

KEYSTORE_PATH="$1"

# Check if keystore exists
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo -e "${RED}Error: Keystore file not found: $KEYSTORE_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}✓${NC} Found keystore: $KEYSTORE_PATH"
echo ""

# Get keystore info
KEYSTORE_SIZE=$(ls -lh "$KEYSTORE_PATH" | awk '{print $5}')
echo "Keystore size: $KEYSTORE_SIZE"
echo ""

# Encode to base64
echo "Encoding to Base64..."
if base64 -i "$KEYSTORE_PATH" > keystore.base64.txt 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Base64 encoding successful"
else
    # Try without -i flag (for Linux)
    base64 "$KEYSTORE_PATH" > keystore.base64.txt
    echo -e "${GREEN}✓${NC} Base64 encoding successful"
fi

echo ""
echo "================================================"
echo "  Setup Instructions"
echo "================================================"
echo ""
echo "1. Go to your GitHub repository"
echo "2. Navigate to: Settings > Secrets and variables > Actions"
echo "3. Click 'New repository secret'"
echo ""
echo "4. Add the following secrets:"
echo ""
echo -e "${YELLOW}Secret Name:${NC} RELEASE_KEYSTORE_BASE64"
echo -e "${YELLOW}Value:${NC} (paste the contents of keystore.base64.txt)"
echo ""
echo -e "${YELLOW}Secret Name:${NC} KEYSTORE_PASSWORD"
echo -e "${YELLOW}Value:${NC} (your keystore password)"
echo ""
echo -e "${YELLOW}Secret Name:${NC} KEY_ALIAS"
echo -e "${YELLOW}Value:${NC} (your key alias name)"
echo ""
echo -e "${YELLOW}Secret Name:${NC} KEY_PASSWORD"
echo -e "${YELLOW}Value:${NC} (your key password)"
echo ""
echo "================================================"
echo ""
echo -e "${GREEN}✓${NC} Base64 encoded keystore saved to: ${YELLOW}keystore.base64.txt${NC}"
echo ""
echo -e "${YELLOW}⚠ WARNING:${NC} This file contains your keystore data!"
echo "   - Do NOT commit this file to git"
echo "   - Delete it after copying to GitHub secrets"
echo "   - Keep your original keystore safe"
echo ""

# Check if file should be copied to clipboard
if command -v pbcopy &> /dev/null; then
    echo -n "Copy to clipboard? (y/N): "
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        cat keystore.base64.txt | pbcopy
        echo -e "${GREEN}✓${NC} Copied to clipboard! You can now paste it into GitHub."
    fi
elif command -v xclip &> /dev/null; then
    echo -n "Copy to clipboard? (y/N): "
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        cat keystore.base64.txt | xclip -selection clipboard
        echo -e "${GREEN}✓${NC} Copied to clipboard! You can now paste it into GitHub."
    fi
else
    echo "Note: Install pbcopy (macOS) or xclip (Linux) to enable clipboard copy"
fi

echo ""
echo "Next steps:"
echo "1. Add secrets to GitHub (see instructions above)"
echo "2. Run the 'Release Build' workflow from GitHub Actions"
echo "3. Delete keystore.base64.txt when done"
echo ""
echo "For more information, see .github/workflows/README.md"
echo ""
