-- Teams. "name" is shown as the creator typed it. "name_key" is the same name in lower case,
-- and its unique constraint is what stops two teams whose names differ only by case.
-- A separate column is used because an index on lower(name) is not portable between databases.
create table teams (
    id         uuid                     not null,
    name       varchar(50)              not null,
    name_key   varchar(50)              not null,
    created_by uuid                     not null,
    created_at timestamp with time zone not null,
    constraint pk_teams primary key (id),
    constraint uq_teams_name_key unique (name_key),
    constraint fk_teams_created_by foreign key (created_by) references users (id)
);

-- Who belongs to which team. The pair is the primary key, so a user cannot be added twice.
create table team_members (
    team_id  uuid                     not null,
    user_id  uuid                     not null,
    added_at timestamp with time zone not null,
    constraint pk_team_members primary key (team_id, user_id),
    constraint fk_team_members_team foreign key (team_id) references teams (id),
    constraint fk_team_members_user foreign key (user_id) references users (id)
);

-- Listing the teams a user belongs to.
create index ix_team_members_user on team_members (user_id);
