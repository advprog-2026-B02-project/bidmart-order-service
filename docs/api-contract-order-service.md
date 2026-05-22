# Order Service API Contract

Dokumen ini merangkum kontrak API Order Service berdasarkan source code yang ada di workspace ini, khususnya controller, DTO, model, dan security filter. Tujuannya adalah menjadi panduan integrasi untuk frontend/BFF.

## 1. Daftar Aktor & Hak Akses

### Ringkasan akses

| Aktor | Tujuan | Endpoint yang dapat diakses | Header / autentikasi wajib |
| --- | --- | --- | --- |
| Buyer / Pembeli | Melihat order milik sendiri, melihat detail order, konfirmasi penerimaan, membuat sengketa | `/api/v1/orders`, `/api/v1/orders/{orderId}`, `/api/v1/orders/{orderId}/receive`, `/api/v1/orders/{orderId}/dispute` | `X-User-Id: <UUID>` |
| Seller / Penjual | Melihat order milik store sendiri, mengisi data pengiriman / resi | `/api/v1/orders`, `/api/v1/orders/{orderId}/ship` | `X-User-Id: <UUID>` |
| Internal / Sistem | Membuat order dari event eksternal (contoh: auction menang) | `/internal/v1/orders` | `X-Service-Token: <secret>` dan `Idempotency-Key: <string>` |
| Admin | Menyelesaikan sengketa, melihat list order (filter by status) | `/admin/v1/orders`, `/admin/v1/orders/{orderId}/dispute/resolve` | `Authorization: Bearer <JWT>` dengan role `ADMIN` |

### Catatan autentikasi

- Endpoint user-facing di bawah `/api/v1/orders/**` secara security config ditandai `permitAll`, tetapi controller dan service tetap mengandalkan `X-User-Id` untuk otorisasi ownership.
- Tidak ada header `X-Seller-Id` pada source code ini. Identitas seller dibaca dari `X-User-Id`, lalu divalidasi apakah cocok dengan `sellerId` pada order.
- Endpoint internal hanya menerima request jika `X-Service-Token` cocok dengan nilai konfigurasi `app.service-token`.
- Endpoint admin diproteksi oleh Spring Security berbasis JWT. Token harus memuat role `ADMIN` agar lolos `hasRole("ADMIN")`.

## 2. State Lifecycle (Status Pesanan)

### Status yang tersedia di source code

Enum status order saat ini adalah:

- `CREATED`
- `SHIPPED`
- `COMPLETED`
- `DISPUTED`
- `RESOLVED`

Tidak ada status `PENDING_PAYMENT` atau `PAID` pada source code ini. Jadi lifecycle yang benar untuk service ini berbeda dari contoh umum marketplace yang punya tahap pembayaran terpisah.

### Alur lifecycle yang terimplementasi

```mermaid
stateDiagram-v2
    [*] --> CREATED: internal create order
  CREATED --> SHIPPED: seller update shipping
    SHIPPED --> COMPLETED: buyer confirm receipt
    SHIPPED --> DISPUTED: buyer create dispute
    DISPUTED --> RESOLVED: admin resolve dispute
```

### Penjelasan transisi

1. `CREATED`
   - Status default saat order pertama kali disimpan.
   - Di-set otomatis pada `@PrePersist` jika status belum diisi.
   - Dibuat melalui endpoint internal `/internal/v1/orders`.

2. `SHIPPED`
   - Di-set oleh seller melalui `PUT /api/v1/orders/{orderId}/ship`.
   - Syarat sebelum update:
     - caller harus seller order tersebut
    - order harus berada pada status `CREATED`
   - Saat sukses, service menyimpan `courier`, `trackingNumber`, dan `shippedAt`.

3. `COMPLETED`
   - Di-set oleh buyer melalui `PUT /api/v1/orders/{orderId}/receive`.
   - Syarat sebelum update:
     - caller harus buyer order tersebut
     - order harus berada pada status `SHIPPED`

4. `DISPUTED`
   - Di-set oleh buyer melalui `POST /api/v1/orders/{orderId}/dispute`.
   - Syarat sebelum update:
     - caller harus buyer order tersebut
     - order harus berada pada status `SHIPPED`
   - Saat sengketa dibuat, service menyimpan:
     - `disputeReason`
     - `disputeDescription`
     - `evidenceImages`
     - `disputedAt`

5. `RESOLVED`
   - Di-set oleh admin melalui `PUT /admin/v1/orders/{orderId}/dispute/resolve`.
   - Syarat sebelum update:
     - order harus berada pada status `DISPUTED`
   - Saat resolusi dibuat, service menyimpan:
     - `disputeResolution`
     - `disputeNote`
     - `resolvedAt`

### Catatan penting integrasi

- Tidak ada endpoint `pay order` pada service ini.
- Tidak ada endpoint `cancel order` pada source code yang saya baca.
- `UpdateShippingRequest` memiliki field `status`, tetapi implementasi service tidak membaca field tersebut; status order tetap diubah server menjadi `SHIPPED` jika request valid.
- `OrderResponse.shipping` dan `OrderResponse.timeline` sudah ada di DTO, tetapi saat ini diisi `null` oleh service.

## 3. Struktur Data (DTO)

### 3.1 `CreateOrder` - request internal create order

Dipakai oleh `POST /internal/v1/orders`.

```json
{
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "listingId": "d0b5e0d3-6b7f-4f1e-bc4f-f0ce7d4f2f10",
  "listingTitle": "Wireless Keyboard",
  "listingImageUrl": "https://cdn.example.com/items/keyboard.png",
  "buyerId": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
  "buyerDisplayName": "Andi",
  "shippingStreet": "Jl. Melati No. 10",
  "shippingCity": "Bandung",
  "shippingProvince": "Jawa Barat",
  "shippingPostalCode": "40123",
  "sellerId": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
  "sellerDisplayName": "Toko Komputer A",
  "totalAmount": 250000
}
```

| Field | Tipe | Required | Nullable | Catatan |
| --- | --- | --- | --- | --- |
| `auctionId` | UUID | Ya | Tidak | Dipakai untuk idempotency bisnis order; unique di database |
| `listingId` | UUID | Ya | Tidak | Disimpan ke order |
| `listingTitle` | string | Ya | Tidak | Disimpan ke order dan notification |
| `listingImageUrl` | string | Tidak | Ya | Jika null, response detail akan menampilkan images kosong |
| `buyerId` | UUID | Ya | Tidak | Dipakai untuk ownership dan notification |
| `buyerDisplayName` | string | Ya | Tidak | Disimpan ke order |
| `shippingStreet` | string | Tidak | Ya | Disimpan ke order |
| `shippingCity` | string | Tidak | Ya | Disimpan ke order |
| `shippingProvince` | string | Tidak | Ya | Disimpan ke order |
| `shippingPostalCode` | string | Tidak | Ya | Disimpan ke order |
| `sellerId` | UUID | Ya | Tidak | Dipakai untuk ownership dan notification |
| `sellerDisplayName` | string | Ya | Tidak | Disimpan ke order |
| `totalAmount` | integer | Ya | Tidak | Disimpan ke order |

### 3.2 `OrderListResponse`

Dipakai oleh `GET /api/v1/orders`.

```json
{
  "content": [
    {
      "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
      "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
      "listingTitle": "Wireless Keyboard",
      "amount": 250000,
      "buyer": {
        "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
        "displayName": "Andi"
      },
      "seller": {
        "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
        "displayName": "Toko Komputer A"
      },
      "status": "CREATED",
      "createdAt": "2026-05-21T12:34:56.123"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

| Field | Tipe | Required | Nullable |
| --- | --- | --- | --- |
| `content` | array of `OrderSummary` | Ya | Tidak |
| `page` | integer | Ya | Tidak |
| `size` | integer | Ya | Tidak |
| `totalElements` | long | Ya | Tidak |
| `totalPages` | integer | Ya | Tidak |

### 3.3 `OrderSummary`

```json
{
  "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "listingTitle": "Wireless Keyboard",
  "amount": 250000,
  "buyer": {
    "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
    "displayName": "Andi"
  },
  "seller": {
    "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
    "displayName": "Toko Komputer A"
  },
  "status": "CREATED",
  "createdAt": "2026-05-21T12:34:56.123"
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `id` | Ya | Tidak | UUID order |
| `auctionId` | Ya | Tidak | UUID auction |
| `listingTitle` | Ya | Tidak | Judul listing |
| `amount` | Ya | Tidak | Total amount |
| `buyer` | Ya | Tidak | Object `UserBasicDTO` |
| `seller` | Ya | Tidak | Object `UserBasicDTO` |
| `status` | Ya | Tidak | String dari enum status |
| `createdAt` | Ya | Tidak | Timestamp pembuatan |

### 3.4 `OrderResponse`

Dipakai oleh `GET /api/v1/orders/{orderId}`.

```json
{
  "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "listing": {
    "id": "d0b5e0d3-6b7f-4f1e-bc4f-f0ce7d4f2f10",
    "title": "Wireless Keyboard",
    "images": ["https://cdn.example.com/items/keyboard.png"]
  },
  "amount": 250000,
  "buyer": {
    "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
    "displayName": "Andi",
    "shippingAddress": {
      "street": "Jl. Melati No. 10",
      "city": "Bandung",
      "province": "Jawa Barat",
      "postalCode": "40123"
    }
  },
  "seller": {
    "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
    "displayName": "Toko Komputer A"
  },
  "status": "CREATED",
  "shipping": null,
  "timeline": null,
  "createdAt": "2026-05-21T12:34:56.123"
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `id` | Ya | Tidak | UUID order |
| `auctionId` | Ya | Tidak | UUID auction |
| `listing` | Ya | Tidak | Object `ListingDTO` |
| `amount` | Ya | Tidak | Total amount |
| `buyer` | Ya | Tidak | Object `BuyerDTO` |
| `seller` | Ya | Tidak | Object `SellerDTO` |
| `status` | Ya | Tidak | String dari enum status |
| `shipping` | Ya secara schema | Ya secara runtime | Saat ini selalu `null` dari service |
| `timeline` | Ya secara schema | Ya secara runtime | Saat ini selalu `null` dari service |
| `createdAt` | Ya | Tidak | Timestamp pembuatan |

#### Nested object: `ListingDTO`

```json
{
  "id": "d0b5e0d3-6b7f-4f1e-bc4f-f0ce7d4f2f10",
  "title": "Wireless Keyboard",
  "images": ["https://cdn.example.com/items/keyboard.png"]
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `id` | Ya | Tidak | UUID listing |
| `title` | Ya | Tidak | Judul listing |
| `images` | Ya | Tidak | Array string; empty array jika `listingImageUrl` null |

#### Nested object: `BuyerDTO`

```json
{
  "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
  "displayName": "Andi",
  "shippingAddress": {
    "street": "Jl. Melati No. 10",
    "city": "Bandung",
    "province": "Jawa Barat",
    "postalCode": "40123"
  }
}
```

| Field | Required | Nullable |
| --- | --- | --- |
| `id` | Ya | Tidak |
| `displayName` | Ya | Tidak |
| `shippingAddress` | Ya | Tidak |

#### Nested object: `ShippingDTO`

DTO ini ada di class, tetapi tidak dipopulasi oleh service saat ini.

```json
{
  "courier": "JNE",
  "trackingNumber": "JNE123456789",
  "shippedAt": "2026-05-21T14:00:00"
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `courier` | Ya secara schema | Ya secara runtime | Tidak diisi oleh `toResponseDTO` saat ini |
| `trackingNumber` | Ya secara schema | Ya secara runtime | Tidak diisi oleh `toResponseDTO` saat ini |
| `shippedAt` | Ya secara schema | Ya secara runtime | Tidak diisi oleh `toResponseDTO` saat ini |

#### Nested object: `TimelineDTO`

DTO ini ada di class, tetapi tidak dipopulasi oleh service saat ini.

```json
{
  "status": "SHIPPED",
  "timestamp": "2026-05-21T14:00:00"
}
```

### 3.5 `UpdateShippingRequest`

Dipakai oleh `PUT /api/v1/orders/{orderId}/ship`.

```json
{
  "status": "SHIPPED",
  "courier": "JNE",
  "trackingNumber": "JNE123456789"
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `status` | Tidak | Ya | Saat ini tidak dipakai oleh service |
| `courier` | Tidak | Ya | Disimpan ke order jika dikirim |
| `trackingNumber` | Tidak | Ya | Disimpan ke order jika dikirim |

### 3.6 `DisputeRequest`

Dipakai oleh `POST /api/v1/orders/{orderId}/dispute`.

```json
{
  "reason": "ITEM_NOT_AS_DESCRIBED",
  "description": "Produk yang diterima tidak sesuai foto",
  "evidenceImages": [
    "https://cdn.example.com/evidence/1.png",
    "https://cdn.example.com/evidence/2.png"
  ]
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `reason` | Tidak secara annotation, tetapi operasionalnya penting | Ya | Disimpan ke `disputeReason` |
| `description` | Tidak | Ya | Disimpan ke `disputeDescription` |
| `evidenceImages` | Tidak | Ya | Disimpan sebagai string CSV di `evidenceImages` |

### 3.7 `ResolveDisputeRequest`

Dipakai oleh `PUT /admin/v1/orders/{orderId}/dispute/resolve`.

```json
{
  "resolution": "REFUND_BUYER",
  "note": "Setelah review, barang tidak sesuai deskripsi"
}
```

| Field | Required | Nullable | Catatan |
| --- | --- | --- | --- |
| `resolution` | Tidak secara annotation, tetapi operasionalnya penting | Ya | Disimpan ke `disputeResolution` |
| `note` | Tidak | Ya | Disimpan ke `disputeNote` |

## 4. Endpoint Specifications

### 4.1 Buyer Endpoints

#### 4.1.1 Get My Orders

- Method: `GET`
- Path: `/api/v1/orders`
- Auth: `X-User-Id: <UUID>`
- Query params:
  - `role=BUYER` or omit; jika omit, service default akan memperlakukan sebagai buyer kecuali role seller dikirim
  - `status` optional, misalnya `CREATED`, `SHIPPED`, `COMPLETED`, `DISPUTED`, `RESOLVED`
  - `page` default `0`
  - `size` default `20`

Example request:

```http
GET /api/v1/orders?role=BUYER&status=CREATED&page=0&size=20 HTTP/1.1
X-User-Id: 2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2
```

Example response:

```json
{
  "content": [
    {
      "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
      "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
      "listingTitle": "Wireless Keyboard",
      "amount": 250000,
      "buyer": {
        "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
        "displayName": "Andi"
      },
      "seller": {
        "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
        "displayName": "Toko Komputer A"
      },
      "status": "CREATED",
      "createdAt": "2026-05-21T12:34:56.123"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### 4.1.2 Get Order Detail

- Method: `GET`
- Path: `/api/v1/orders/{orderId}`
- Auth: `X-User-Id: <UUID>`

Example request:

```http
GET /api/v1/orders/8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c HTTP/1.1
X-User-Id: 2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2
```

Example response:

```json
{
  "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "listing": {
    "id": "d0b5e0d3-6b7f-4f1e-bc4f-f0ce7d4f2f10",
    "title": "Wireless Keyboard",
    "images": ["https://cdn.example.com/items/keyboard.png"]
  },
  "amount": 250000,
  "buyer": {
    "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
    "displayName": "Andi",
    "shippingAddress": {
      "street": "Jl. Melati No. 10",
      "city": "Bandung",
      "province": "Jawa Barat",
      "postalCode": "40123"
    }
  },
  "seller": {
    "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
    "displayName": "Toko Komputer A"
  },
  "status": "CREATED",
  "shipping": null,
  "timeline": null,
  "createdAt": "2026-05-21T12:34:56.123"
}
```

#### 4.1.3 Complete Order / Confirm Receipt

- Method: `PUT`
- Path: `/api/v1/orders/{orderId}/receive`
- Auth: `X-User-Id: <UUID>`

Example request:

```http
PUT /api/v1/orders/8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c/receive HTTP/1.1
X-User-Id: 2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2
```

Example response:

```http
HTTP/1.1 200 OK
```

No response body.

#### 4.1.4 Create Dispute

- Method: `POST`
- Path: `/api/v1/orders/{orderId}/dispute`
- Auth: `X-User-Id: <UUID>`

Example request:

```http
POST /api/v1/orders/8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c/dispute HTTP/1.1
X-User-Id: 2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2
Content-Type: application/json

{
  "reason": "ITEM_NOT_AS_DESCRIBED",
  "description": "Produk yang diterima tidak sesuai foto",
  "evidenceImages": [
    "https://cdn.example.com/evidence/1.png",
    "https://cdn.example.com/evidence/2.png"
  ]
}
```

Example response:

```http
HTTP/1.1 200 OK
```

No response body.

### 4.2 Seller Endpoints

#### 4.2.1 Get Store Orders

- Method: `GET`
- Path: `/api/v1/orders`
- Auth: `X-User-Id: <UUID>`
- Query params:
  - `role=SELLER`
  - `status` optional
  - `page` default `0`
  - `size` default `20`

Example request:

```http
GET /api/v1/orders?role=SELLER&status=CREATED&page=0&size=20 HTTP/1.1
X-User-Id: 7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d
```

Example response:

```json
{
  "content": [
    {
      "id": "8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c",
      "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
      "listingTitle": "Wireless Keyboard",
      "amount": 250000,
      "buyer": {
        "id": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
        "displayName": "Andi"
      },
      "seller": {
        "id": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
        "displayName": "Toko Komputer A"
      },
      "status": "CREATED",
      "createdAt": "2026-05-21T12:34:56.123"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### 4.2.2 Update Resi / Ship Order

- Method: `PUT`
- Path: `/api/v1/orders/{orderId}/ship`
- Auth: `X-User-Id: <UUID>`

Example request:

```http
PUT /api/v1/orders/8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c/ship HTTP/1.1
X-User-Id: 7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d
Content-Type: application/json

{
  "status": "SHIPPED",
  "courier": "JNE",
  "trackingNumber": "JNE123456789"
}
```

Example response:

```http
HTTP/1.1 200 OK
```

No response body.

Catatan: service akan menolak request jika order belum `CREATED` atau jika `X-User-Id` bukan seller order tersebut.

### 4.3 Internal Endpoints

#### 4.3.1 Create Order From Event

- Method: `POST`
- Path: `/internal/v1/orders`
- Auth:
  - `X-Service-Token: <secret>`
  - `Idempotency-Key: <string>`

Example request:

```http
POST /internal/v1/orders HTTP/1.1
X-Service-Token: super-secret-token
Idempotency-Key: auction-evt-9b6f7c2d
Content-Type: application/json

{
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "listingId": "d0b5e0d3-6b7f-4f1e-bc4f-f0ce7d4f2f10",
  "listingTitle": "Wireless Keyboard",
  "listingImageUrl": "https://cdn.example.com/items/keyboard.png",
  "buyerId": "2f0b5fcb-66b8-45e9-9db7-9f65e4e1b8e2",
  "buyerDisplayName": "Andi",
  "shippingStreet": "Jl. Melati No. 10",
  "shippingCity": "Bandung",
  "shippingProvince": "Jawa Barat",
  "shippingPostalCode": "40123",
  "sellerId": "7df9a8ef-2f19-4f6c-bdbe-43eb6d8a2a9d",
  "sellerDisplayName": "Toko Komputer A",
  "totalAmount": 250000
}
```

Example response:

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "auctionId": "3d2e8e7b-0f8f-4d72-a0d7-6d4f6d4d1d53",
  "status": "CREATED",
  "createdAt": "2026-05-21T12:34:56.123"
}
```

Catatan:

- Response body berbentuk `Map<String, Object>`, bukan DTO khusus.
- Service akan memeriksa duplikasi `auctionId` dan duplikasi `Idempotency-Key`.

### 4.4 Admin Endpoints

#### 4.4.1 List Orders (Admin)

- Method: `GET`
- Path: `/admin/v1/orders`
- Auth: `Authorization: Bearer <JWT>` dengan role `ADMIN`

Query params:
- `status` optional — filter by `OrderStatus` (e.g. `DISPUTED`). If invalid, returns `400 Bad Request`.
- `page` optional — default `0`
- `size` optional — default `20`

Response: `OrderListResponse` (same shape as `GET /api/v1/orders`).

Example request:

```http
GET /admin/v1/orders?status=DISPUTED&page=0&size=20 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Example response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [ /* OrderSummary items */ ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

Errors:
- `400 Bad Request` when `status` is not a valid `OrderStatus`.
- `403 Forbidden` when caller token does not contain `ADMIN` role.

Implementation notes:
- Implemented in `AdminOrderController#getOrders(...)` and `OrderService.getOrdersAdmin(...)` in this codebase. It supports paging and optional status filtering.
- Tests added: `src/test/java/id/ac/ui/cs/advprog/bidmart/order/controller/AdminOrderControllerGetOrdersTest.java` (integration-style `MockMvc` tests for admin access and authorization).

#### 4.4.2 Resolve Dispute

- Method: `PUT`
- Path: `/admin/v1/orders/{orderId}/dispute/resolve`
- Auth: `Authorization: Bearer <JWT>` dengan role `ADMIN`

Example request:

```http
PUT /admin/v1/orders/8f6fb8f6-39f8-4d6f-80d1-0ef9f7cb2c4c/dispute/resolve HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "resolution": "REFUND_BUYER",
  "note": "Setelah review, barang tidak sesuai deskripsi"
}
```

Example response:

```http
HTTP/1.1 200 OK
```

No response body.

## 5. Error Codes & Edge Cases

### 5.1 Error code matrix

| Status | Kondisi | Sumber di source code |
| --- | --- | --- |
| 400 Bad Request | Status order tidak valid di query `status` | `parseStatus()` |
| 400 Bad Request | Buyer mencoba konfirmasi penerimaan saat order belum `SHIPPED` | `confirmReceipt()` |
| 400 Bad Request | Seller mencoba ship saat order belum `CREATED` | `updateShipping()` |
| 400 Bad Request | Buyer membuat dispute saat order belum `SHIPPED` | `createDispute()` |
| 400 Bad Request | Admin resolve dispute saat order belum `DISPUTED` | `resolveDispute()` |
| 401 Unauthorized | `X-Service-Token` missing / salah pada internal endpoint | `ServiceTokenFilter` |
| 403 Forbidden | User bukan pemilik order saat akses detail / update / confirm / dispute | `getOrderById()`, `updateShipping()`, `confirmReceipt()`, `createDispute()` |
| 403 Forbidden | Caller bukan admin pada endpoint resolve dispute | Spring Security `hasRole("ADMIN")` |
| 404 Not Found | Order ID tidak ditemukan | `findOrderOrThrow()` |
| 409 Conflict | `Idempotency-Key` sudah pernah dipakai | `createOrderFromEvent()` |
| 409 Conflict | `auctionId` sudah memiliki order | `createOrderFromEvent()` |

### 5.2 Edge cases yang perlu diperhatikan frontend

1. Missing atau invalid `X-User-Id` pada endpoint user-facing tidak memiliki handler khusus di service layer. Dalam praktik, Spring MVC biasanya mengembalikan 400 untuk header yang wajib tetapi tidak ada.
2. `GET /api/v1/orders` memakai parameter `role` untuk menentukan jalur query seller atau buyer. Jika `role=SELLER`, hasil difilter berdasarkan `sellerId`; selain itu dianggap buyer path.
3. Jika `status` dikirim dan tidak cocok dengan enum `OrderStatus`, service akan membalas 400.
4. `GET /api/v1/orders/{orderId}` hanya mengembalikan order jika caller adalah buyer atau seller order tersebut.
5. `PUT /api/v1/orders/{orderId}/ship` tidak membaca field `status` di body. Frontend boleh mengirimnya untuk kelengkapan payload, tetapi status aktual tetap ditentukan server.
6. `OrderResponse.shipping` dan `OrderResponse.timeline` ada di schema tetapi saat ini selalu `null`; frontend jangan bergantung pada data tersebut untuk rendering utama.
7. `evidenceImages` pada dispute disimpan sebagai string CSV di database. Di API request tetap berupa array string.
8. Setelah dispute di-resolve, service tidak menyediakan endpoint lanjutan untuk mengembalikan status ke `COMPLETED` atau membatalkan resolusi.

## 6. Rekomendasi Integrasi Frontend

- Simpan `X-User-Id` di BFF/session layer dan kirim konsisten ke semua endpoint user-facing.
- Saat membuka detail order, gunakan `status` dari response untuk menentukan CTA yang tampil, misalnya `receive order`, `ship order`, atau `create dispute`.
- Untuk internal event flow, gunakan `Idempotency-Key` yang stabil per event agar retry tidak membuat order duplikat.
- Untuk admin flow, pastikan JWT yang dipakai mengandung role `ADMIN` karena endpoint tidak menerima `X-User-Id`.
