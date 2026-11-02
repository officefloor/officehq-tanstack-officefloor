package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retire form; {@code date} is null when left blank (the sheet's today). */
public record RetireInput(LocalDate date) {
}
