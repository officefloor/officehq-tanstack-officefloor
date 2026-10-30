package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A flag that passed validation: a non-blank reason, an until date and the steward who flags. */
public record ValidFlag(String reason, LocalDate until, String user) {
}
