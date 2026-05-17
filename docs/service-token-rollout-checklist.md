# Service Token Rollout Checklist

Checklist ini untuk memastikan rollout `SERVICE_TOKEN` lintas lingkungan aman.

## Local
- [ ] `SERVICE_TOKEN` terisi di `.env` lokal.
- [ ] Endpoint internal order reject request tanpa header `X-Service-Token`.
- [ ] Service pemanggil internal endpoint sudah mengirim `X-Service-Token`.

## CI/CD
- [ ] GitHub secret `DOCKERHUB_USERNAME` sudah ada.
- [ ] GitHub secret `DOCKERHUB_TOKEN` sudah ada.
- [ ] Workflow `.github/workflows/ci-cd.yml` sukses di branch `staging` dan `main`.

## Production
- [ ] Secret `bidmart/prod/service-token` tersedia di AWS Secrets Manager.
- [ ] ECS task definition inject env `SERVICE_TOKEN` dari ARN secret.
- [ ] Task execution role punya akses `secretsmanager:GetSecretValue`.
- [ ] Semua service internal memakai token yang sama per environment.

## Rotation
- [ ] Token lama ditandai (owner + tanggal dibuat).
- [ ] Token baru diterapkan ke semua consumer.
- [ ] Token lama dicabut setelah grace period.
- [ ] Insiden kebocoran token dicatat dan ditutup.
