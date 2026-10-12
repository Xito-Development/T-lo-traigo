#!/usr/bin/env bash
# Publica T lo traigo (macOS/Linux). Uso: ./scripts/publicar.sh [parche|menor|mayor] ["mensaje"]
set -euo pipefail
cd "$(dirname "$0")/.."
REPO=https://github.com/Xito-Development/T-lo-traigo.git
TIPO=${1:-parche}; MSG=${2:-}
command -v gh >/dev/null || { echo "Instala GitHub CLI: https://cli.github.com"; exit 1; }
gh auth status >/dev/null 2>&1 || gh auth login --web --git-protocol https
gh auth setup-git
[ -d .git ] || git init -b main
git remote get-url origin >/dev/null 2>&1 && git remote set-url origin "$REPO" || git remote add origin "$REPO"
git fetch origin --tags -q || true
if git ls-remote --heads origin main | grep -q main && ! git rev-parse -q --verify HEAD >/dev/null; then git fetch origin main -q; git reset --soft origin/main; fi
CUR=$(grep '^VERSION_NAME=' version.properties | cut -d= -f2)
if git ls-remote --tags origin "refs/tags/v$CUR" | grep -q .; then
  IFS=. read -r MA MI PA <<< "${CUR%%-*}"
  case $TIPO in mayor) NEW="$((MA+1)).0.0";; menor) NEW="$MA.$((MI+1)).0";; *) NEW="$MA.$MI.$((PA+1))";; esac
else NEW=$CUR; fi
[ -n "$MSG" ] || read -rp "¿Qué ha cambiado en la versión $NEW? " MSG
MSG=${MSG:-Mejoras y correcciones.}
printf 'VERSION_NAME=%s' "$NEW" > version.properties
grep -q "## \[$NEW\]" CHANGELOG.md || awk -v e="## [$NEW] - $(date +%F)\n### Cambiado\n- $MSG\n" 'BEGIN{d=0} /^## \[/ && !d {print e; d=1} {print}' CHANGELOG.md > CHANGELOG.tmp && mv CHANGELOG.tmp CHANGELOG.md
printf '%s\n' "${MSG:0:480}" > fastlane/metadata/android/es-ES/changelogs/default.txt
git add -A; git diff --cached --quiet || git commit -qm "Versión $NEW: $MSG"
git push -u origin main
echo "Versión $NEW enviada. Release en ~8 min: https://github.com/Xito-Development/T-lo-traigo/releases/latest"
