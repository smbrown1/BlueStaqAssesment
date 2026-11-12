-- Which notes are shared with which teams. The pair is the primary key, so a note
-- cannot be shared with the same team twice. Deleting a note removes its shares.
create table note_shares (
    note_id   uuid                     not null,
    team_id   uuid                     not null,
    shared_at timestamp with time zone not null,
    constraint pk_note_shares primary key (note_id, team_id),
    constraint fk_note_shares_note foreign key (note_id) references notes (id) on delete cascade,
    constraint fk_note_shares_team foreign key (team_id) references teams (id)
);

-- Listing the notes shared with a team.
create index ix_note_shares_team on note_shares (team_id);
