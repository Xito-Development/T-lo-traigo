#!/usr/bin/env bash
# (macOS/Linux) Crea la clave de firma y la sube como secretos al repo de Xito-Development.
set -euo pipefail
REPO=Xito-Development/T-lo-traigo; DIR="$HOME/TLoTraigo-firma"; mkdir -p "$DIR"
PASS=$(openssl rand -base64 24 | tr -d '/+=')
[ -f "$DIR/release.jks" ] && PASS=$(cat "$DIR/contraseña.txt") || {
  keytool -genkeypair -keystore "$DIR/release.jks" -alias tlotraigo -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$PASS" -keypass "$PASS" -dname "CN=Xito Development, O=Xito Development, L=Leon, C=ES"
  printf '%s' "$PASS" > "$DIR/contraseña.txt"; }
base64 < "$DIR/release.jks" | tr -d '\n' | gh secret set KEYSTORE_BASE64 -R $REPO
gh secret set KEYSTORE_PASSWORD -R $REPO -b "$PASS"; gh secret set KEY_PASSWORD -R $REPO -b "$PASS"; gh secret set KEY_ALIAS -R $REPO -b tlotraigo
echo "Listo. Haz copia de seguridad de $DIR"
