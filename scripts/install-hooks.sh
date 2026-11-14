#!/bin/bash
# Script to install NezhaEDR local Git hooks

set -e

REPO_ROOT=$(git rev-parse --show-toplevel)

echo "🔧 Installing NezhaEDR local Git hooks..."

chmod +x "$REPO_ROOT/.githooks/pre-commit"
git config core.hooksPath "$REPO_ROOT/.githooks"

echo "✅ Git hooks installed successfully! Pre-commit hooks will now enforce branch naming and YAML syntax."
