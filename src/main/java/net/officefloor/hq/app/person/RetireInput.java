package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retirement: the date already defaulted to the sheet's today. */
public record RetireInput(LocalDate from, String user) {
}
