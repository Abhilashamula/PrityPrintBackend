# Ping & Print backend

Spring Boot 3 API for authenticated document upload, server-priced print orders,
Razorpay payments, and durable cloud printing through a provider abstraction.
The first cloud provider is Epson Connect API v2; existing `LOCAL_AGENT` printer
records remain supported and are not routed through Epson.

## Prerequisites

- Java 17 and Maven 3.9, or Docker Desktop
- PostgreSQL 15+
- Razorpay test credentials and webhook secret
- Epson Connect API v2 application credentials for real cloud printing

## Local startup

Create a `.env` beside `docker-compose.yml` (it is ignored by Git):

```env
JWT_SECRET=replace-with-at-least-32-random-bytes
ADMIN_USERNAME=admin
ADMIN_PASSWORD=replace-this-password
TOKEN_ENCRYPTION_KEY=replace-with-a-base64-encoded-32-byte-key

RAZORPAY_KEY_ID=rzp_test_replace_me
RAZORPAY_KEY_SECRET=replace_me
RAZORPAY_WEBHOOK_SECRET=replace_me

EPSON_API_KEY=replace_me
EPSON_CLIENT_ID=replace_me
EPSON_CLIENT_SECRET=replace_me
EPSON_REDIRECT_URI=http://localhost:8080/api/epson/oauth/callback
FRONTEND_URL=http://localhost:5173
```

Generate the token-encryption key in PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Start PostgreSQL and the API:

```powershell
docker compose up --build
Invoke-RestMethod http://localhost:8080/api/health
```

Flyway applies the existing V1-V9 migrations automatically. Do not edit applied
migrations. Documents are stored under the configured private storage directory;
the API does not publish permanent document URLs.

## Frontend configuration

Use these values in the frontend `.env`:

```env
VITE_API_URL=http://localhost:8080/api
VITE_RAZORPAY_KEY_ID=rzp_test_replace_me
```

Only the Razorpay key ID belongs in the browser. Razorpay secrets, Epson
credentials, Epson access/refresh tokens, and Epson upload URIs stay on the API.

## Epson onboarding

1. Log in to the existing admin dashboard.
2. Add a printer with provider `EPSON_CONNECT`.
3. Select it and choose **Connect Epson**.
4. Complete Epson authorization. The callback stores encrypted tokens and loads
   device information and document capabilities.
5. Choose **Refresh capabilities**, then configure currently loaded media. Media
   choices come from Epson's response; source identifiers are never invented.
6. Set per-page B&W/color prices and enable only media physically loaded now.
7. Use **Test connection**, then enable the printer.

For the EcoTank L18050, configure its actual rear-feed media one combination at a
time. Duplex, landscape, and custom page ranges are not advertised for the Epson
path unless a verified implementation is added later.

## Payment and print lifecycle

The API creates the internal order and authoritative amount before Razorpay
Checkout. Browser signature verification links the payment, while a verified
`payment.captured` webhook marks the order paid and creates at most one print
job. The database-backed worker claims queued jobs transactionally, creates one
Epson job, uploads the private document, executes printing, and polls provider
status. Ambiguous create/execute timeouts become `STATUS_UNKNOWN`; they are not
blindly retried or automatically refunded.

Configure Razorpay's webhook URL as:

```text
https://your-api.example.com/api/payments/razorpay/webhook
```

Subscribe at minimum to payment captured/failed and refund events supported by
the account. The webhook URL must be publicly reachable over HTTPS.

## Verification

```powershell
docker run --rm -v pingprint-m2:/root/.m2 -v "${PWD}:/workspace" -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -B test
```

Before production, run a controlled Razorpay test payment and a one-page PDF on
the authorized physical printer. Confirm exactly one `payment_transactions` row,
one internal `print_jobs` row, and one Epson provider job ID for the order.

Production also needs HTTPS, managed secret storage, database backups, private
document retention/deletion policy, log monitoring, and malware scanning if files
come from users outside the trusted campus population.
