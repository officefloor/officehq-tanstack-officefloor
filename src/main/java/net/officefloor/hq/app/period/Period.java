package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A sheet's period: a name unique on the sheet, from and to dates inclusive. {@code closedTotal} is the
 * due-total fixed when it was closed (null while it is open), {@code personTotals} each person's due-total for
 * it fixed then, by person id (empty while it is open; a person added later has none). A {@code soft}-closed
 * period (not {@code closed}) has its figures fixed the same way, but its claims can still change.
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals, boolean soft) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null, Map.of(), false);
    }

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }

    /** Whether the date falls inside the period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** The period closed at its people's figures; its own total is their sum. */
    public Period closedAt(Map<Long, BigDecimal> totals) {
        BigDecimal total = totals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Period(sheetId, name, from, to, true, total, totals, false);
    }

    /** The period soft-closed at its people's figures; its own total is their sum. */
    public Period softClosedAt(Map<Long, BigDecimal> totals) {
        BigDecimal total = totals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Period(sheetId, name, from, to, false, total, totals, true);
    }

    public Period withPersonTotals(Map<Long, BigDecimal> totals) {
        return new Period(sheetId, name, from, to, closed, closedTotal, totals, soft);
    }

    public Period reopened() {
        return new Period(sheetId, name, from, to, false, null, Map.of(), false);
    }
}
