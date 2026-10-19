package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period of a sheet: a name unique on the sheet, and its from and to dates, both inclusive. A closed
 *  period keeps the due-total it had when it was closed (closedTotal; null while open). */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null);
    }

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }

    /** Whether the date falls in the period, its dates included. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    public Period close(BigDecimal total) {
        return new Period(sheetId, name, from, to, true, total);
    }
}
