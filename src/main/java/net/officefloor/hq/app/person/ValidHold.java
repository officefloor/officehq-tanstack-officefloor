package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold form that passed validation. A null from takes the sheet's today; a null to is open-ended. */
public record ValidHold(LocalDate from, LocalDate to, String user) {
}
