package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold form; {@code from} is null when left blank (the sheet's today), {@code to} null
 *  when left blank (open-ended). */
public record HoldInput(LocalDate from, LocalDate to) {
}
