package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A flag once validated: its reason (trimmed, not blank), its until-date (null when none was given, so the sheet's
 *  today), and who flags. */
public record Flag(String reason, LocalDate until, String by) {
}
