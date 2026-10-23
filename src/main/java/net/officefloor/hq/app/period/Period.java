package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's period: a name unique on the sheet and an inclusive from and to date; whether it is closed, and
 *  the due-total fixed when it was closed (null while open); and whether it is soft-closed: its due-total (and each
 *  person's) fixed as a close fixes them, while its claims can still be changed (never both closed and soft); and the
 *  most OPEN or PENDING claims a person may have in it (null: no cap). */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, boolean soft, Integer cap) {

    /** Whether this period shares a day with the dates from..to (inclusive). */
    public boolean overlaps(LocalDate from, LocalDate to) {
        return !this.from.isAfter(to) && !from.isAfter(this.to);
    }

    /** Whether the date falls in this period. */
    public boolean contains(LocalDate date) {
        return overlaps(date, date);
    }
}
