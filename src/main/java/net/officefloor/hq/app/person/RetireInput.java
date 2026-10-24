package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retire request: the date the person is retired from (null for the sheet's today). */
public record RetireInput(LocalDate date) {
}
