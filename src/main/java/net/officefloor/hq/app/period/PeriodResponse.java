package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** A period as the front end reads it: its name, its from and to dates (yyyy-MM-dd, inclusive), whether
 *  it is closed and, when closed, the due-total fixed on closing (two decimals; null while open) and each
 *  person's due-total fixed on closing, by person id (empty while open; a person missing shows 0.00), and
 *  whether it is soft-closed: not closed, but with its figures fixed (total and people) as when it was soft-closed,
 *  and the most OPEN or PENDING claims a person may have in it (null for no cap), and each person's carried figure
 *  fixed with their due-total, by person id (empty while open; a person missing shows 0.00), and whether a steward
 *  has locked it (a locked period cannot be reopened), and its place in the order the periods were added (0 where
 *  not read from the store). */
public record PeriodResponse(String name, String from, String to, boolean closed, String total,
        Map<Long, String> people, boolean soft, Integer cap, Map<Long, String> carried, boolean locked, long added) {
    public static PeriodResponse of(String name, LocalDate from, LocalDate to, Integer cap) {
        return of(name, from, to, false, null, Map.of(), false, cap, Map.of());
    }

    public static PeriodResponse of(String name, LocalDate from, LocalDate to, boolean closed, BigDecimal total,
            Map<Long, BigDecimal> people, boolean soft, Integer cap, Map<Long, BigDecimal> carried) {
        return of(name, from, to, closed, total, people, soft, cap, carried, false);
    }

    public static PeriodResponse of(String name, LocalDate from, LocalDate to, boolean closed, BigDecimal total,
            Map<Long, BigDecimal> people, boolean soft, Integer cap, Map<Long, BigDecimal> carried, boolean locked) {
        return new PeriodResponse(name, from.toString(), to.toString(), closed,
                total == null ? null : total.setScale(2).toPlainString(), shown(people), soft, cap, shown(carried),
                locked, 0);
    }

    /** This period with its place in the order the periods were added. */
    public PeriodResponse withAdded(long added) {
        return new PeriodResponse(name, from, to, closed, total, people, soft, cap, carried, locked, added);
    }

    private static Map<Long, String> shown(Map<Long, BigDecimal> figures) {
        Map<Long, String> shown = new LinkedHashMap<>();
        figures.forEach((id, t) -> shown.put(id, t.setScale(2).toPlainString()));
        return shown;
    }
}
