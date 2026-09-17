# Ping & Print backend

Spring Boot API for user accounts, email verification, wallet accounting, print orders, and future PayPal payments.

## Why this stack

- Spring Boot matches the team's Java/Spring experience and gives strong validation/security defaults.
- PostgreSQL is a good fit for users, orders, and money because transactions and row locks matter.
- Flyway keeps schema changes versioned.
- JWT access tokens keep the kiosk frontend stateless; passwords are stored only as BCrypt hashes.
- PayPal should be called only from this API. Never expose PayPal secrets in Vite variables.

## Prerequisites

- Java 17 or newer
- Maven 3.9 or newer
- Docker Desktop

The repository also includes a Docker path, so Java and Maven do not need to be installed on the host machine.

## Run locally

```powershell
docker compose up --build
```

The API starts at `http://localhost:8080`.

PostgreSQL is available to pgAdmin at:

- Host: `localhost`
- Port: `5432`
- Database: `pingprint`
- Username: `pingprint`
- Password: `pingprint_dev_only`

For a real pgAdmin-managed PostgreSQL installation instead, stop the Compose database service and set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from your pgAdmin connection before starting the backend.

To run the backend container against PostgreSQL installed on Windows, create the `pingprint` database in pgAdmin, then run:

```powershell
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "your-password"
docker compose -f docker-compose.external-db.yml up --build -d
```

The external database must allow connections from Docker Desktop. The compose file uses `host.docker.internal` instead of `localhost` because `localhost` inside a container refers to the container itself.

Verify startup with:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## Initial endpoints

- `POST /api/auth/signup` - create account with name, age, gender, phone, and email
- `POST /api/auth/login` - receive a JWT after email verification
- `POST /api/auth/verify-email` - verify a signup token
- `GET /api/auth/me` - get the authenticated profile
- `GET /api/wallet` - get authenticated wallet balance and recent transactions
- `POST /api/print/guest-session` - create a print session without an account
- `GET /api/print/guest-session/{sessionId}` - read a guest session
- `POST /api/payments/paypal/create-order` - reserved for server-side PayPal order creation
- `POST /api/payments/paypal/webhook` - reserved for verified PayPal webhook events (currently disabled)

Example signup:

```json
{
  "email": "student@example.com",
  "password": "a-strong-password",
  "name": "Student",
  "age": 20,
  "gender": "prefer_not_to_say",
  "phone": "+919876543210"
}
```

Login returns:

```json
{
  "accessToken": "jwt-token",
  "user": {
    "id": "user-id",
    "email": "student@example.com",
    "name": "Student",
    "age": 20,
    "gender": "prefer_not_to_say",
    "phone": "+919876543210",
    "emailVerified": true,
    "wallet": { "balanceMinor": 0, "currency": "INR" }
  }
}
```

Send the token on protected requests:

```http
Authorization: Bearer jwt-token
```

Guest users can continue printing through `/api/print/guest-session`; they do not receive a user JWT or wallet. Payment and actual document upload should be attached to the returned session in the next print-order slice.

## Frontend connection

Add this to the frontend `.env`:

```env
VITE_API_URL=http://localhost:8080/api
```

The frontend should call `/auth/signup`, verify the token delivered by your email provider, then call `/auth/login`. Keep the returned access token in memory, send it as `Authorization: Bearer <token>`, and call `/auth/me` on app startup. Use an httpOnly refresh cookie before production; localStorage is not recommended for long-lived tokens on a shared kiosk. During local development the verification token is logged by the backend until an email provider is configured.

For Google sign-up, create a Google OAuth Web client ID with `http://localhost:5173` as an authorized JavaScript origin, then set `VITE_GOOGLE_CLIENT_ID` in the frontend and `GOOGLE_CLIENT_ID` in the backend environment. The backend verifies the Google ID token before issuing the Ping &amp; Print JWT. Google does not reliably provide age, gender, or phone, so the signup form collects those fields before the Google button is used.

## Payment design

PayPal order creation and capture must happen on this backend. The browser starts a payment session, the backend creates the PayPal order, and the backend verifies the webhook/capture before crediting the wallet. Wallet balance changes must be transactional and idempotent.
