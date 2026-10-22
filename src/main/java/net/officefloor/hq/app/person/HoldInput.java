package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold. The from-date is null when the sheet's today is to be used; the to-date is null when the hold
 *  is open-ended. */
public record HoldInput(String user, LocalDate from, LocalDate to) {
}
