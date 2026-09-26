#!/usr/bin/env bash
# Fails if the git index holds anything that could leak a key or personal data:
# built apps, databases, keystores, PDFs (a saved QR key), local-only resources,
# or a full QR key secret. Runs in CI and as the pre-commit hook (.githooks/).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

status=0

forbidden_files=$(git ls-files --cached | grep -E \
  '\.(apk|aab|db|db-shm|db-wal|sqlite|jks|keystore|p12|pdf)$|(^|/)keystore\.properties$|^app/src/debug/res/' \
  || true)
if [ -n "$forbidden_files" ]; then
  echo "✗ These files must not be committed (keys, key databases, builds or personal files):"
  echo "$forbidden_files" | sed 's/^/    /'
  status=1
fi

# A real QR key is livefree://key/v1/ followed by a 32-byte secret (43 base64url chars).
if git grep --cached -nIE 'livefree://key/v1/[A-Za-z0-9_-]{43}' -- . ':!scripts/check-no-secrets.sh'; then
  echo "✗ A QR key secret is in the files above. Remove it: anyone with it can unlock."
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "✓ No keys, key databases or personal files in the commit."
fi
exit "$status"
