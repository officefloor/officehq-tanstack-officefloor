package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A period of a sheet: its name (unique on the sheet) and its from and to dates, both inclusive. A closed
 * period keeps the due-total fixed when it was closed ({@code closedTotal}; null while open) and each person's
 * due-total for it then ({@code personTotals}, by person id; empty while open).
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals) {

    /** A new, open period. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null, Map.of());
    }

    /** Whether the period shares a day with the other. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to()) && !other.from().isAfter(to);
    }

    /** Whether the date falls in the period, both ends included. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** The same period, closed with its due-total and each person's due-total fixed. */
    public Period close(BigDecimal total, Map<Long, BigDecimal> perPerson) {
        return new Period(sheetId, name, from, to, true, total, Map.copyOf(perPerson));
    }

    /** The same period with these fixed per-person due-totals. */
    public Period withPersonTotals(Map<Long, BigDecimal> perPerson) {
        return new Period(sheetId, name, from, to, closed, closedTotal, Map.copyOf(perPerson));
    }

    /** The same period, open again with its due-total live. */
    public Period reopen() {
        return new Period(sheetId, name, from, to, false, null, Map.of());
    }
}
