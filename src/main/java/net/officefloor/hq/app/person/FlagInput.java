package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated flag request: the trimmed, non-blank reason and the until-date (null: the sheet's today). */
public record FlagInput(String reason, LocalDate until) {
}
