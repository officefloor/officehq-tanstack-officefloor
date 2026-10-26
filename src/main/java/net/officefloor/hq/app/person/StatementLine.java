package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One of a person's statement lines: the date the statement was taken (as of), and their due-total then (null: n/a). */
public record StatementLine(LocalDate date, BigDecimal dueTotal) {

    /** The line as shown: the date, then the due-total ("n/a" when none). */
    public String line() {
        return date + " " + (dueTotal == null ? "n/a" : dueTotal.toPlainString());
    }
}
