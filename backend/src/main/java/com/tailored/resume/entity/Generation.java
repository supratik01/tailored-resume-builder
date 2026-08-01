package com.tailored.resume.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generations", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "resume_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Generation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "job_description_id", nullable = false)
    private UUID jobDescriptionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String tailoredJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String atsAnalysisJson;

    @Column(nullable = false)
    private Integer atsScore;

    /**
     * The same scoring formula run against the untouched source resume, before tailoring.
     * Lets the results screen show a real before/after delta instead of inventing one.
     * Nullable so existing rows (scored before this field existed) degrade to "no delta shown"
     * rather than a backfilled guess.
     */
    private Integer baselineScore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
