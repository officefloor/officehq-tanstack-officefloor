package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period of a sheet: its name, unique on the sheet, and its from and to dates, inclusive. A
 *  closed period keeps the due-total it had when it was closed (`closedTotal`). A soft-closed one
 *  (`closed` and `soft`) keeps it too, but claims dated in it can still be changed. */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal,
        boolean soft) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed, BigDecimal closedTotal) {
        this(sheetId, name, from, to, closed, closedTotal, false);
    }

    /** A new, open period. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null);
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
