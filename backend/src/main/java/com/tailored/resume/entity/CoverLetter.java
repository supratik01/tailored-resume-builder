package com.tailored.resume.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One cover letter per generation. Regenerating overwrites the body in place —
 * there is no draft history in v1.
 */
@Entity
@Table(name = "cover_letters", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "generation_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverLetter {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "generation_id", nullable = false, unique = true)
    private UUID generationId;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private String tone;

    /**
     * Comma-separated terms the letter cites that the resume does not evidence, or null when
     * clean. Survives a corrective retry, so a value here means the model insisted.
     */
    @Column(name = "unsupported_terms")
    private String unsupportedTerms;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
