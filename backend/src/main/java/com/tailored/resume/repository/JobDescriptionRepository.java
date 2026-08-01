package com.tailored.resume.repository;

import com.tailored.resume.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {
}
