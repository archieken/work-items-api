package com.elemental.tech.workitems.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkItemRequest(
        @NotBlank @Size(max=120) String title,
        String description,
        @NotNull Priority priority
) {

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
