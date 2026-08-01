package com.tailored.resume.dto.coverletter;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Both fields are optional. {@code tone} defaults to "professional" when absent.
 * {@code notes} is free text the candidate wants reflected (e.g. a referral, a
 * relocation plan) — it is passed to the model as candidate-supplied context.
 */
public record CoverLetterRequest(

        @Pattern(regexp = "professional|warm|direct", message = "tone must be one of: professional, warm, direct")
        String tone,

        @Size(max = 1000, message = "notes must be 1000 characters or fewer")
        String notes
) {
    public String toneOrDefault() {
        return tone == null || tone.isBlank() ? "professional" : tone;
    }
}
