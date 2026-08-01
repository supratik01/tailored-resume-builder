package com.tailored.resume.dto.resume;

import java.util.List;

public record ParsedResume(
        ContactInfo contact,
        String summary,
        List<String> skills,
        List<Experience> experience,
        List<Education> education,
        List<Project> projects,
        List<String> certifications
) {
    public record ContactInfo(String fullName, String email, String phone, String location, String linkedin, String website) {}
    public record Experience(String company, String title, String location, String startDate, String endDate, List<String> bullets) {}
    public record Education(String institution, String degree, String field, String startDate, String endDate, String details) {}
    public record Project(String name, String description, List<String> bullets, List<String> tech) {}
}
