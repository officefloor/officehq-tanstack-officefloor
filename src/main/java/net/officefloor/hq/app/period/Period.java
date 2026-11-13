package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A period of a sheet: its name (unique on the sheet) and its from and to dates, both inclusive. A closed
 * period keeps the due-total fixed when it was closed ({@code closedTotal}; null while open).
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal) {

    /** A new, open period. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null);
    }

    /** Whether the period shares a day with the other. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to()) && !other.from().isAfter(to);
    }

    /** Whether the date falls in the period, both ends included. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** The same period, closed with its due-total fixed. */
    public Period close(BigDecimal total) {
        return new Period(sheetId, name, from, to, true, total);
    }

    /** The same period, open again with its due-total live. */
    public Period reopen() {
        return new Period(sheetId, name, from, to, false, null);
    }
}
