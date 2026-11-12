package com.bluestaq.notes.team;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

	boolean existsByNameKey(String nameKey);

}
