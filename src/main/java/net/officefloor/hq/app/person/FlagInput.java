package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag: its reason (trimmed, not blank) and until-date (null: the sheet's today). */
public record FlagInput(String reason, LocalDate until) {
}
