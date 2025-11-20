#!/bin/bash

# Start Web UI Script
# This script starts a simple web server for the Loan Origination Web UI

echo "🚀 Starting Loan Origination Web UI..."
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    echo "✓ Python 3 found"
    echo "Starting server on http://localhost:3000"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    cd "$(dirname "$0")"
    python3 -m http.server 3000
elif command -v python &> /dev/null; then
    echo "✓ Python found"
    echo "Starting server on http://localhost:3000"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    cd "$(dirname "$0")"
    python -m http.server 3000
elif command -v npx &> /dev/null; then
    echo "✓ npx found"
    echo "Starting server on http://localhost:3000"
    echo ""
    echo "Press Ctrl+C to stop the server"
    echo ""
    cd "$(dirname "$0")"
    npx http-server -p 3000
else
    echo "❌ Error: No suitable web server found"
    echo ""
    echo "Please install one of the following:"
    echo "  - Python 3: https://www.python.org/downloads/"
    echo "  - Node.js/npx: https://nodejs.org/"
    echo ""
    echo "Or use any static file server to serve the web-ui directory"
    exit 1
fi


