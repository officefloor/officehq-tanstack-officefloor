package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period of a sheet: its name and its from and to dates, both inclusive. */
public record Period(long id, String sheetId, String name, LocalDate from, LocalDate to) {
}
