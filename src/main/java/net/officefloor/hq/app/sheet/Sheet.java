package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/** A sheet, as the store holds it. {@code today} is the sheet's date, used instead of the clock. */
public record Sheet(String id, LocalDate today) {
}
