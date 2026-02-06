param (
    [string]$Mode
)

$APP_NAME = "signOn"
$KEYSTORE_DIR = ".\keystore"
$KEYSTORE_PATH = "$KEYSTORE_DIR\signon.p12"
$DATA_DIR = ".\data"
$LOGS_DIR = ".\logs"

Write-Host "=== $APP_NAME setup ==="

# -------------------------
# Install mode
# -------------------------
if ($Mode -eq "install") {
    Write-Host "Running INSTALL mode"

    New-Item -ItemType Directory -Force -Path $KEYSTORE_DIR | Out-Null
    New-Item -ItemType Directory -Force -Path $DATA_DIR | Out-Null
    New-Item -ItemType Directory -Force -Path $LOGS_DIR | Out-Null

    if (-Not (Test-Path $KEYSTORE_PATH)) {
        Write-Host "Creating TLS keystore..."

        keytool -genkeypair `
            -alias signon `
            -keyalg RSA `
            -keysize 2048 `
            -storetype PKCS12 `
            -keystore $KEYSTORE_PATH `
            -validity 365 `
            -dname "CN=localhost"
    }
    else {
        Write-Host "Keystore already exists"
    }
}

# -------------------------
# Restart mode
# -------------------------
if ($Mode -eq "restart") {
    Write-Host "Running RESTART mode"
}

# -------------------------
# Export SSL vars
# -------------------------
$env:SERVER_SSL_ENABLED = "true"
$env:SERVER_SSL_KEY_STORE = (Resolve-Path $KEYSTORE_PATH)
$env:SERVER_SSL_KEY_STORE_TYPE = "PKCS12"

$securePwd = Read-Host "Enter keystore password" -AsSecureString
$env:SERVER_SSL_KEY_STORE_PASSWORD =
    [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePwd)
    )

# -------------------------
# Start app
# -------------------------
Write-Host "Starting application..."
.\mvnw spring-boot:run
