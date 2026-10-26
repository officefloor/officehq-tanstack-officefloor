package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A sheet's period: a name unique on the sheet, from and to dates inclusive. {@code closedTotal} is the
 * due-total fixed when it was closed (null while it is open).
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

    /** Whether the date falls inside the period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    public Period closedAt(BigDecimal total) {
        return new Period(sheetId, name, from, to, true, total);
    }
}
