package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A period on a sheet: its name (unique on the sheet), its from and to dates, inclusive, whether it
 * is closed, and the due-total fixed when it was closed (null while open). A soft-closed period is
 * closed with {@code soft} set: its figures are fixed, but claims dated inside it can still change.
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, boolean soft) {

    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null, false);
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
        return new Period(sheetId, name, from, to, true, total, false);
    }

    /** This period, soft-closed with its due-total fixed. */
    public Period softClose(BigDecimal total) {
        return new Period(sheetId, name, from, to, true, total, true);
    }

    /** Whether it is closed for good: claims dated inside it cannot change. */
    public boolean hardClosed() {
        return closed && !soft;
    }

    /** This period, reopened: its due-total live again. */
    public Period reopen() {
        return new Period(sheetId, name, from, to, false, null, false);
    }
}
