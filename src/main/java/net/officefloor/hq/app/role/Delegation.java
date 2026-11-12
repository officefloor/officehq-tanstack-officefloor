package net.officefloor.hq.app.role;

import java.time.LocalDate;

/** The steward role delegated to {@code user} by the steward {@code by} for from..to, inclusive. */
public record Delegation(String user, LocalDate from, LocalDate to, String by) {
}
