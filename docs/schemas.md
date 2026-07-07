# Schemas

Two layers: the database table backing everything, and the JSON request/response
shapes the frontend actually talks to. Field names differ slightly between them
(DB uses `snake_case`, JSON uses `camelCase`).

## Database: `short_urls` table

Defined in `src/main/resources/db/migration/V1__init_schema.sql`.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | no | Primary key, not exposed in any API response |
| `original_url` | `TEXT` | no | The **normalized** URL (see BACKEND.md) |
| `short_code` | `VARCHAR(12)` | no | Either an 8-character Base62 generated code or a 6-12 character custom alias, `UNIQUE` |
| `created_at` | `TIMESTAMPTZ` | no | Set server-side on creation |
| `expires_at` | `TIMESTAMPTZ` | yes | Defaults to `created_at + 365 days` if not specified (see "Data retention" in BACKEND.md); rows are permanently purged some time after this passes |
| `click_count` | `BIGINT` | no | Starts at 0, incremented on each successful redirect |

```sql
CREATE TABLE short_urls (
    id              BIGSERIAL PRIMARY KEY,
    original_url    TEXT NOT NULL,
    short_code      VARCHAR(12) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NULL,
    click_count     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_short_urls_short_code UNIQUE (short_code)
);
```

## JSON: request/response DTOs

Java source of truth: `src/main/java/com/urlshortener/web/dto/`.

### `ShortenRequest` — body of `POST /api/urls`

```json
{
  "url": "https://example.com/some/long/path",
  "expiresAt": "2026-12-31T23:59:59Z",
  "customAlias": "mylink12"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `url` | string | **yes** | Must not be blank, and must be a real `http`/`https` URL — scheme is optional (`https://` assumed if missing), but non-web schemes, gibberish text, and file-like hosts (`.exe`, `.pdf`, etc.) are rejected with `400`. See BACKEND.md "Validation rules". |
| `expiresAt` | ISO-8601 datetime string | no | If omitted, a default expiry is applied automatically (365 days by default — see "Data retention" in BACKEND.md), **not** "never expires". Not validated to be in the future — a past timestamp creates an already-expired link. |
| `customAlias` | string | no | 6-12 alphanumeric characters. If available, used directly as the short code. If already taken, the request **silently falls back** to standard hash-based generation instead of erroring — check the returned `shortCode` to see whether your alias was actually used. |

### `ShortenResponse` — body of `201` from `POST /api/urls`

```json
{
  "shortCode": "a1B2c3D4",
  "shortUrl": "http://localhost:8080/a1B2c3D4",
  "originalUrl": "https://example.com/some/long/path",
  "createdAt": "2026-07-07T10:15:30Z",
  "expiresAt": "2027-07-07T10:15:30Z"
}
```

| Field | Type | Notes |
|---|---|---|
| `shortCode` | string | 6-12 chars, `[0-9a-zA-Z]` — 8 chars if generated, 6-12 if your custom alias was honored |
| `shortUrl` | string | Full redirect URL — `{host}/{shortCode}` — ready to display/copy |
| `originalUrl` | string | The **normalized** form of what was submitted, not necessarily byte-identical to the input |
| `createdAt` | ISO-8601 datetime | Server-assigned |
| `expiresAt` | ISO-8601 datetime | Either what you submitted, or the auto-assigned default (never `null` for a newly-created link) |

### `ShortUrlStatsResponse` — body of `200` from `GET /api/urls/{shortCode}`

```json
{
  "shortCode": "a1B2c3D4",
  "originalUrl": "https://example.com/some/long/path",
  "createdAt": "2026-07-07T10:15:30Z",
  "expiresAt": "2027-07-07T10:15:30Z",
  "clickCount": 42
}
```

Same shape as `ShortenResponse` but with `clickCount` instead of `shortUrl`, and it's
returned even if the link has already expired (expiry only blocks the redirect
endpoint, not stats).

### `ErrorResponse` — body of every non-2xx response

```json
{
  "error": "No short URL found for code: missing"
}
```

Single `error` string field. Message text is human-readable but not a stable enum —
don't pattern-match on it in the frontend; use the HTTP status code to branch logic.

## HTTP status code reference

| Status | When | Which endpoint |
|---|---|---|
| `201 Created` | Short URL created or dedup match returned | `POST /api/urls` |
| `302 Found` | Valid, non-expired code | `GET /{shortCode}` |
| `200 OK` | Code exists (expired or not) | `GET /api/urls/{shortCode}`, `GET /api/urls/{shortCode}/qr` |
| `400 Bad Request` | Blank/invalid `url`, malformed input | `POST /api/urls` |
| `404 Not Found` | Unknown `shortCode` | `GET /{shortCode}`, `GET /api/urls/{shortCode}`, `GET /api/urls/{shortCode}/qr` |
| `410 Gone` | Code exists but `expiresAt` is in the past | `GET /{shortCode}` only (QR generation does not check expiry) |
| `500 Internal Server Error` | Short-code generation exhausted all collision retries (extremely rare) | `POST /api/urls` |

## QR code endpoint

`GET /api/urls/{shortCode}/qr` returns raw `image/png` bytes (not JSON) — a 300x300px
PNG encoding the full `shortUrl` link (the same string `POST /api/urls` returns as
`shortUrl`). No request/response DTO; the body is the image itself. Works regardless
of whether the link has expired.
