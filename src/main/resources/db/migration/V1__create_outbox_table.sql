create table idempotency_keys (
    id uuid primary key,
    "key" varchar(255) not null unique,
    order_id uuid,
    auction_id uuid,
    created_at timestamp not null
);

create index idx_idempotency_keys_auction_id on idempotency_keys (auction_id);

create table orders (
    id uuid primary key,
    auction_id uuid not null unique,
    listing_id uuid not null,
    listing_title varchar(255) not null,
    listing_image_url varchar(500),
    buyer_id uuid not null,
    buyer_display_name varchar(100) not null,
    shipping_street varchar(255),
    shipping_city varchar(100),
    shipping_province varchar(100),
    shipping_postal_code varchar(10),
    courier varchar(100),
    tracking_number varchar(255),
    shipped_at timestamp,
    seller_id uuid not null,
    seller_display_name varchar(100) not null,
    status varchar(30) not null,
    total_amount integer not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    dispute_reason varchar(255),
    dispute_description text,
    disputed_at timestamp,
    dispute_resolution varchar(50),
    dispute_note text,
    resolved_at timestamp,
    evidence_images text
);

create index idx_orders_buyer_id on orders (buyer_id);
create index idx_orders_seller_id on orders (seller_id);
create index idx_orders_status_created_at on orders (status, created_at);

create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100),
    event_type varchar(100) not null,
    topic varchar(255) not null,
    message_key varchar(255),
    payload text not null,
    status varchar(20) not null,
    attempts integer not null default 0,
    last_error text,
    created_at timestamp not null,
    updated_at timestamp not null,
    dispatched_at timestamp
);

create index idx_outbox_events_status_created_at on outbox_events (status, created_at);
create index idx_outbox_events_status_attempts on outbox_events (status, attempts);