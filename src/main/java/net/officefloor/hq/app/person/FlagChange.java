package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag: its reason (trimmed, never blank), its until-date and who makes the change. */
public record FlagChange(String reason, LocalDate until, String user) {
}
