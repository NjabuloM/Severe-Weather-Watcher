package dev.njabulo.spotter;

import java.time.LocalDateTime;

public record Headline(LocalDateTime effectiveDate, int effectiveEpochDate, int severity, String headline, String category) {
}
