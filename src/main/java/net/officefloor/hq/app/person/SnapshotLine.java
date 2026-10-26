package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One of a person's snapshot lines: the day being left as the sheet's today moved on, and their due-total then
 *  (null: n/a). */
public record SnapshotLine(LocalDate day, BigDecimal dueTotal) {

    /** The line as shown: the day, then the due-total ("n/a" when none). */
    public String line() {
        return day + " " + (dueTotal == null ? "n/a" : dueTotal.toPlainString());
    }
}
