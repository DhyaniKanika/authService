param (
    [ValidateSet("install", "restart")]
    [string]$Action = "restart",

    [ValidateSet("dev", "prod")]
    [string]$Env = "prod"
)

Write-Host "SignOn setup starting ($Action / $Env)"

# -----------------------------
# Java 17 check
# -----------------------------
$javaVersion = & java -version 2>&1
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch "17") {
    Write-Error "Java 17 is required. Install JDK 17 and ensure java is on PATH."
    exit 1
}

# -----------------------------
# Paths
# -----------------------------
$BaseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$KeystoreDir = Join-Path $BaseDir "keystore"
$DataDir = Join-Path $BaseDir "data"
$KeystorePath = Join-Path $KeystoreDir "signon.p12"

New-Item -ItemType Directory -Force -Path $KeystoreDir | Out-Null
New-Item -ItemType Directory -Force -Path $DataDir | Out-Null

# -----------------------------
# Keystore creation (install only)
# -----------------------------
if ($Action -eq "install" -and -not (Test-Path $KeystorePath)) {
    Write-Host "Creating TLS keystore..."

    & keytool `
        -genkeypair `
        -alias signon `
        -keyalg RSA `
        -keysize 2048 `
        -storetype PKCS12 `
        -keystore $KeystorePath `
        -validity 3650 `
        -dname "CN=signon, OU=Security, O=SignOn, L=Local, S=NA, C=XX" `
        -storepass changeit `
        -keypass changeit
}

# -----------------------------
# Environment variables
# -----------------------------
$env:SPRING_PROFILES_ACTIVE = $Env
$env:SERVER_SSL_ENABLED = "true"
$env:SERVER_SSL_KEY_STORE = $KeystorePath
$env:SERVER_SSL_KEY_STORE_TYPE = "PKCS12"
$env:SERVER_SSL_KEY_STORE_PASSWORD = "changeit"

# -----------------------------
# Start application
# -----------------------------
Write-Host "Starting application..."
.\mvnw.cmd spring-boot:run
