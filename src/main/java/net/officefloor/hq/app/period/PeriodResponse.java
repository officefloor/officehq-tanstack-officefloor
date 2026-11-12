package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** A period as the front end reads it: its name, its from and to dates (yyyy-MM-dd, inclusive), whether
 *  it is closed and, when closed, the due-total fixed on closing (two decimals; null while open) and each
 *  person's due-total fixed on closing, by person id (empty while open; a person missing shows 0.00), and
 *  whether it is soft-closed: not closed, but with its figures fixed (total and people) as when it was soft-closed. */
public record PeriodResponse(String name, String from, String to, boolean closed, String total,
        Map<Long, String> people, boolean soft) {
    public static PeriodResponse of(String name, LocalDate from, LocalDate to) {
        return of(name, from, to, false, null, Map.of(), false);
    }

    public static PeriodResponse of(String name, LocalDate from, LocalDate to, boolean closed, BigDecimal total,
            Map<Long, BigDecimal> people, boolean soft) {
        Map<Long, String> shown = new LinkedHashMap<>();
        people.forEach((id, t) -> shown.put(id, t.setScale(2).toPlainString()));
        return new PeriodResponse(name, from.toString(), to.toString(), closed,
                total == null ? null : total.setScale(2).toPlainString(), shown, soft);
    }
}
