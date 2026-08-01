package com.tailored.resume.repository;

import com.tailored.resume.entity.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, UUID> {
    Optional<CoverLetter> findByGenerationIdAndUserId(UUID generationId, UUID userId);
}
