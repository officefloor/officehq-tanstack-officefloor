package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retire. The date is null when the sheet's today is to be used. */
public record RetireInput(String user, LocalDate from) {
}
