package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag form: a reason (not blank) and the date it is in force until. */
public record FlagInput(String reason, LocalDate until) {
}
