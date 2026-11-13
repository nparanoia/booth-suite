package com.booth.agent.chat;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(@NotBlank String runId) {
}
