# Pure Backend

## Local Docker startup

This service requires `APP_ENCRYPTION_KEY_BASE64` for AES/GCM sensitive-data encryption.

- Expected format: Base64-encoded raw AES key
- Valid decoded lengths: `16`, `24`, or `32` bytes

`docker-compose.yml` now includes a local-dev fallback so startup works out of the box:

- `APP_ENCRYPTION_KEY_BASE64=${APP_ENCRYPTION_KEY_BASE64:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}`

That fallback decodes to a 32-byte key for local demo use. For production, always provide your own secret key via environment or secret manager.

## Run

```bash
docker compose up --build
```

## Override encryption key (recommended)

Set your own key before startup:

```bash
export APP_ENCRYPTION_KEY_BASE64="$(openssl rand -base64 32)"
docker compose up --build
```

Windows PowerShell:

```powershell
$env:APP_ENCRYPTION_KEY_BASE64 = [Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))
docker compose up --build
```

## Verify startup

1. `docker compose ps` shows `practice_backend` as `running`.
2. `docker compose logs app --tail=200` has no `APP_ENCRYPTION_KEY_BASE64 must be set` error.
3. App responds on `http://localhost:8080`.
