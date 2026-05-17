# Docker Hub and Service Token Runbook

Dokumen ini memusatkan langkah untuk:
1. Push image `bidmart-order-service` ke Docker Hub.
2. Konfigurasi GitHub Actions agar auto push image.
3. Menyimpan `SERVICE_TOKEN` di AWS Secrets Manager dan inject ke ECS.
4. Menyelaraskan token yang sama di service internal lain.

## 1) Manual push image ke Docker Hub

```bash
docker login -u <DOCKERHUB_USERNAME>
docker build -t <DOCKERHUB_USERNAME>/bidmart-order-service:latest .
docker push <DOCKERHUB_USERNAME>/bidmart-order-service:latest
```

Opsional tag tambahan:

```bash
docker tag <DOCKERHUB_USERNAME>/bidmart-order-service:latest <DOCKERHUB_USERNAME>/bidmart-order-service:staging
docker push <DOCKERHUB_USERNAME>/bidmart-order-service:staging
```

## 2) GitHub Secrets yang wajib ada

Di GitHub repository, set dua secret berikut:
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

Workflow yang digunakan: `.github/workflows/ci-cd.yml`
- Branch `staging`: push tag `:staging`
- Branch `main`: push tag `:latest`

## 3) Simpan SERVICE_TOKEN di AWS Secrets Manager

Contoh membuat secret (sekali):

```bash
aws secretsmanager create-secret \
  --name bidmart/prod/service-token \
  --description "Shared internal service token for BidMart microservices" \
  --secret-string '{"SERVICE_TOKEN":"<YOUR_TOKEN>"}'
```

Jika secret sudah ada, update nilai:

```bash
aws secretsmanager put-secret-value \
  --secret-id bidmart/prod/service-token \
  --secret-string '{"SERVICE_TOKEN":"<NEW_TOKEN>"}'
```

## 4) Inject SERVICE_TOKEN ke ECS Task Definition

Di container definition service order, tambahkan `secrets`:

```json
[
  {
    "name": "SERVICE_TOKEN",
    "valueFrom": "arn:aws:secretsmanager:<region>:<account-id>:secret:bidmart/prod/service-token-xxxx"
  }
]
```

Pastikan Task Execution Role punya permission:
- `secretsmanager:GetSecretValue`
- `kms:Decrypt` (jika secret dienkripsi KMS custom)

## 5) Kontrak lintas service internal

Semua pemanggil endpoint `/internal/**` harus kirim header berikut:
- `X-Service-Token: <SERVICE_TOKEN>`

Service yang perlu disinkronkan:
- `order-service` (validator incoming token untuk `/internal/**`)
- `notification-service` (terima/pakai token untuk internal endpoint)
- `bidding-service` (jika call internal endpoint order)
- `catalog-service` (jika call internal endpoint lintas service)

## 6) Rotasi token (tanpa downtime)

Strategi yang disarankan:
1. Buat token baru (`T2`) dan simpan di Secrets Manager.
2. Deploy consumer services agar kirim `T2`.
3. Update validator service agar menerima `T1` dan `T2` sementara (grace period).
4. Setelah semua service confirmed ke `T2`, revoke `T1`.
5. Catat waktu rotasi dan owner pada dokumen operasional.

Catatan: implementasi dual-token validation belum ada di codebase ini; saat ini validasi memakai satu nilai token.
