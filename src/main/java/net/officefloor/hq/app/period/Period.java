package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A period on a sheet: its name (unique on the sheet), its from and to dates, inclusive, whether it
 * is closed, and the due-total fixed when it was closed (null while open).
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null);
    }

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }

    /** Whether the date falls in the period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** This period, closed with its due-total fixed. */
    public Period close(BigDecimal total) {
        return new Period(sheetId, name, from, to, true, total);
    }
}
