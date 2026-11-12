package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period as the front end reads it: its name, its from and to dates (yyyy-MM-dd, inclusive), whether
 *  it is closed and, when closed, the due-total fixed on closing (two decimals; null while open). */
public record PeriodResponse(String name, String from, String to, boolean closed, String total) {
    public static PeriodResponse of(String name, LocalDate from, LocalDate to) {
        return of(name, from, to, false, null);
    }

    public static PeriodResponse of(String name, LocalDate from, LocalDate to, boolean closed, BigDecimal total) {
        return new PeriodResponse(name, from.toString(), to.toString(), closed,
                total == null ? null : total.setScale(2).toPlainString());
    }
}
