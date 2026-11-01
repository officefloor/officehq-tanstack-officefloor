package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period of a sheet: its name, unique on the sheet, and its from and to dates, inclusive. A
 *  closed period keeps the due-total it had when it was closed (`closedTotal`). A soft-closed one
 *  (`closed` and `soft`) keeps it too, but claims dated in it can still be changed. A period may cap
 *  how many OPEN or PENDING claims due by in it each person may have (`cap`; null for none). A steward
 *  may lock a period (`locked`): a locked period cannot be reopened. */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal,
        boolean soft, Integer cap, boolean locked) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal,
            boolean soft, Integer cap) {
        this(sheetId, name, from, to, closed, closedTotal, soft, cap, false);
    }

    public Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal,
            boolean soft) {
        this(sheetId, name, from, to, closed, closedTotal, soft, null);
    }

    public Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal) {
        this(sheetId, name, from, to, closed, closedTotal, false);
    }

    /** A new, open period. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null);
    }

    /** A new, open period with a cap (null for none). */
    public Period(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        this(sheetId, name, from, to, false, null, false, cap);
    }

    /** The period, locked or unlocked. */
    public Period withLocked(boolean locked) {
        return new Period(sheetId, name, from, to, closed, closedTotal, soft, cap, locked);
    }

    /** Whether the period is closed outright, not soft-closed: no claim dated in it can change. */
    public boolean hardClosed() {
        return closed && !soft;
    }

    /** Whether the two periods share a day. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to) && !other.from.isAfter(to);
    }

    /** Whether the date falls in the period. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
