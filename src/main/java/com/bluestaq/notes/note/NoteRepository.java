package com.bluestaq.notes.note;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {

	Page<Note> findByOwnerId(UUID ownerId, Pageable pageable);

}
