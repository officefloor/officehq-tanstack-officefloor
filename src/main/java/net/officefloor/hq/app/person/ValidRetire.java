package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A retire form that passed validation. A null date takes the sheet's today. */
public record ValidRetire(LocalDate date, String user) {
}
