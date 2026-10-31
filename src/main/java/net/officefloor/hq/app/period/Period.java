package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period of a sheet: its name, unique on the sheet, and its from and to dates, inclusive. */
public record Period(String sheetId, String name, LocalDate from, LocalDate to) {

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }
}
