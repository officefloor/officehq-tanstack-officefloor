package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A sheet's period: a name unique on the sheet, from and to dates inclusive. */
public record Period(String sheetId, String name, LocalDate from, LocalDate to) {

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }
}
