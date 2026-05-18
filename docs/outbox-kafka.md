# Outbox + Kafka Flow

Order service now writes notification intents to an `outbox_events` table first, then a background dispatcher publishes them to Kafka.

## Why this exists

- Prevents lost notifications when order creation succeeds but direct delivery fails.
- Keeps the order write and the notification request in the same database transaction.
- Lets the notification service consume events asynchronously from Kafka.

## Topic

- `order.notification-requests`

## Message contract

Kafka message value is the serialized `SaveNotification` JSON.

Kafka headers include:

- `eventId`
- `aggregateType`
- `aggregateId`
- `eventType`

## Outbox schema

The `outbox_events` table stores:

- aggregate type and ID
- event type
- topic
- Kafka key
- JSON payload
- delivery status
- retry attempts
- error message
- timestamps

## Runtime flow

1. Order service creates or updates order data.
2. `NotificationServiceClient` serializes `SaveNotification` and stores it as an outbox row.
3. `OutboxDispatcher` polls pending rows.
4. Dispatcher publishes each row to Kafka.
5. On success, row status becomes `SENT`.
6. On failure, attempts increase and the row stays `PENDING` until max retries, then becomes `FAILED`.

## Notes

- `KafkaConfig` is disabled in `test` profile.
- `OutboxDispatcher` is also disabled in `test` profile.
- The consumer side should still be idempotent and use the Kafka key or `eventId` header to deduplicate messages.