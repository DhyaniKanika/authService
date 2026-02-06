#!/bin/bash
set -e

APP_NAME="signOn"
KEYSTORE_DIR="./keystore"
KEYSTORE_PATH="$KEYSTORE_DIR/signon.p12"
DATA_DIR="./data"

echo "=== $APP_NAME setup ==="

# -------------------------
# Install mode
# -------------------------
if [ "$1" == "install" ]; then
  echo "Running INSTALL mode"

  mkdir -p "$KEYSTORE_DIR"
  mkdir -p "$DATA_DIR"
  mkdir -p logs

  if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "Creating TLS keystore..."

    keytool -genkeypair \
      -alias signon \
      -keyalg RSA \
      -keysize 2048 \
      -storetype PKCS12 \
      -keystore "$KEYSTORE_PATH" \
      -validity 365 \
      -dname "CN=localhost"
  else
    echo "Keystore already exists"
  fi
fi

# -------------------------
# Restart mode
# -------------------------
if [ "$1" == "restart" ]; then
  echo "Running RESTART mode"
fi

# -------------------------
# Export SSL vars
# -------------------------
export SERVER_SSL_ENABLED=true
export SERVER_SSL_KEY_STORE="$(pwd)/$KEYSTORE_PATH"
export SERVER_SSL_KEY_STORE_TYPE=PKCS12

read -s -p "Enter keystore password: " SERVER_SSL_KEY_STORE_PASSWORD
echo
export SERVER_SSL_KEY_STORE_PASSWORD

# -------------------------
# Start app
# -------------------------
echo "Starting application..."
./mvnw spring-boot:run
