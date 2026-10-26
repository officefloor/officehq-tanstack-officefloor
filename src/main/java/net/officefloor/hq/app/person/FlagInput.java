package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag: its trimmed reason and the date it holds until (inclusive). */
public record FlagInput(String reason, LocalDate until) {
}
