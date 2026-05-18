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