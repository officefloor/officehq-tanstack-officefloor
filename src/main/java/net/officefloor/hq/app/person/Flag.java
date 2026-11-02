package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** The flag to set on a person: its reason (trimmed, not blank) and its until-date. */
public record Flag(String reason, LocalDate until) {
}
