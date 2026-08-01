package com.tailored.resume.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_descriptions", indexes = @Index(columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String title;

    private String company;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
