package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retire body. The date is null when none was given (it defaults to the sheet's today). */
public record RetireInput(LocalDate from) {
}
