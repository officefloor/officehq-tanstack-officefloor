package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold once validated: its from-date (null when none was given, so the sheet's today), its to-date (null: open-ended),
 *  and who adds it. */
public record Hold(LocalDate from, LocalDate to, String by) {
}
