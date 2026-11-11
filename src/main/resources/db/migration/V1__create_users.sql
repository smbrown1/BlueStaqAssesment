-- Registered users. Usernames are stored in lower case, so the unique
-- constraint also prevents two users whose names differ only by case.
create table users (
    id            uuid                     not null,
    username      varchar(50)              not null,
    password_hash varchar(100)             not null,
    created_at    timestamp with time zone not null,
    constraint pk_users primary key (id),
    constraint uq_users_username unique (username)
);
