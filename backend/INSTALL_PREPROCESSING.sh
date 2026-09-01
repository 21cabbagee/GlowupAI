#!/bin/bash
# Installation script for image preprocessing dependencies

echo "=========================================="
echo "Image Preprocessing Setup"
echo "=========================================="
echo ""

# Check if we're in the right directory
if [ ! -f "pyproject.toml" ]; then
    echo "Error: Please run this script from the backend directory"
    exit 1
fi

echo "Installing preprocessing dependencies..."
echo ""

# Try to install using pip
if command -v pip &> /dev/null; then
    echo "Using pip to install opencv-python and numpy..."
    pip install opencv-python>=4.8.0 numpy>=1.24.0
    INSTALL_STATUS=$?
elif command -v pip3 &> /dev/null; then
    echo "Using pip3 to install opencv-python and numpy..."
    pip3 install opencv-python>=4.8.0 numpy>=1.24.0
    INSTALL_STATUS=$?
elif command -v python3 &> /dev/null; then
    echo "Using python3 -m pip to install..."
    python3 -m pip install opencv-python>=4.8.0 numpy>=1.24.0
    INSTALL_STATUS=$?
else
    echo "Error: No pip or python3 found. Please install Python first."
    exit 1
fi

echo ""
if [ $INSTALL_STATUS -eq 0 ]; then
    echo "✓ Dependencies installed successfully!"
    echo ""
    echo "Running verification script..."
    echo ""
    python3 verify_preprocessing_setup.py
else
    echo "⚠ Installation encountered issues."
    echo ""
    echo "You may need to:"
    echo "  1. Activate your virtual environment first"
    echo "  2. Use: source venv/bin/activate"
    echo "  3. Then run: pip install opencv-python numpy"
    echo ""
    echo "Or if using system Python with restrictions:"
    echo "  python3 -m pip install --user opencv-python numpy"
fi

echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Set environment variable:"
echo "   export SKINPROOF_ENABLE_PREPROCESSING=1"
echo ""
echo "2. Run tests:"
echo "   python3 -m pytest tests/test_preprocessing.py -v"
echo ""
echo "3. Start the server and test preprocessing"
echo ""
echo "See PREPROCESSING_IMPLEMENTATION.txt for details"
echo "=========================================="
