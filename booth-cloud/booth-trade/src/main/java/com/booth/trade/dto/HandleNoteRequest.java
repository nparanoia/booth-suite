package com.booth.trade.dto;

import jakarta.validation.constraints.Size;

public record HandleNoteRequest(@Size(max = 500) String note) {
}
