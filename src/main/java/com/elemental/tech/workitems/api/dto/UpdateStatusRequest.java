package com.elemental.tech.workitems.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull Status status,
        @NotNull Long version
) {

    public enum Status {
        OPEN,
        IN_PROGRESS,
        DONE,
        CANCELLED
    }
}
