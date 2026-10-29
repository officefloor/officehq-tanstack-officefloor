package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A sheet's period: a name unique on the sheet, from and to dates inclusive. {@code closedTotal} is the
 * due-total fixed when it was closed (null while it is open), {@code personTotals} each person's due-total for
 * it fixed then, by person id (empty while it is open; a person added later has none), and {@code personCarried} each
 * person's carried figure for it fixed then the same way. A {@code soft}-closed
 * period (not {@code closed}) has its figures fixed the same way, but its claims can still change. {@code cap} is how
 * many OPEN or PENDING claims each person may have in it (null: no cap). A {@code locked} period cannot be reopened.
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried, boolean soft,
        Integer cap, boolean locked) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, null);
    }

    public Period(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        this(sheetId, name, from, to, false, null, Map.of(), Map.of(), false, cap, false);
    }

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }

    /** Whether the date falls inside the period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** The period closed at its people's figures and carried figures; its own total is the sum of their figures. */
    public Period closedAt(Map<Long, BigDecimal> totals, Map<Long, BigDecimal> carried) {
        BigDecimal total = totals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Period(sheetId, name, from, to, true, total, totals, carried, false, cap, locked);
    }

    /** The period soft-closed at its people's figures and carried figures; its own total is the sum of their figures. */
    public Period softClosedAt(Map<Long, BigDecimal> totals, Map<Long, BigDecimal> carried) {
        BigDecimal total = totals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Period(sheetId, name, from, to, false, total, totals, carried, true, cap, locked);
    }

    public Period withPersonTotals(Map<Long, BigDecimal> totals, Map<Long, BigDecimal> carried) {
        return new Period(sheetId, name, from, to, closed, closedTotal, totals, carried, soft, cap, locked);
    }

    public Period reopened() {
        return new Period(sheetId, name, from, to, false, null, Map.of(), Map.of(), false, cap, locked);
    }

    public Period withLocked(boolean locked) {
        return new Period(sheetId, name, from, to, closed, closedTotal, personTotals, personCarried, soft, cap, locked);
    }
}
