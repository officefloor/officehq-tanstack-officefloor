package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag: its trimmed reason and the date it is in force until (inclusive). */
public record Flag(String reason, LocalDate until) {
}
