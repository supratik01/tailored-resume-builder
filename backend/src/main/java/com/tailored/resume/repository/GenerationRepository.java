package com.tailored.resume.repository;

import com.tailored.resume.entity.Generation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRepository extends JpaRepository<Generation, UUID> {
    List<Generation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Generation> findByIdAndUserId(UUID id, UUID userId);
    long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant from);
}
