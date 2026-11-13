package com.booth.agent.chat;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(@NotBlank String ticketId) {
}
