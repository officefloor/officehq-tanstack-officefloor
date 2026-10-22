package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A sheet's period: a name unique on the sheet and an inclusive from and to date. */
public record Period(String sheetId, String name, LocalDate from, LocalDate to) {

    /** Whether this period shares a day with the dates from..to (inclusive). */
    public boolean overlaps(LocalDate from, LocalDate to) {
        return !this.from.isAfter(to) && !from.isAfter(this.to);
    }
}
