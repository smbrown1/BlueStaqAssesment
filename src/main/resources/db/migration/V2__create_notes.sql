-- Notes. Each belongs to the user who wrote it.
-- "version" counts saves; an update based on a stale version is rejected.
create table notes (
    id         uuid                     not null,
    owner_id   uuid                     not null,
    title      varchar(200)             not null,
    body       varchar(10000)           not null,
    version    bigint                   not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_notes primary key (id),
    constraint fk_notes_owner foreign key (owner_id) references users (id)
);

-- Listing a user's notes, most recently updated first.
create index ix_notes_owner_updated on notes (owner_id, updated_at);
