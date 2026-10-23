package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

/** A period as the screen shows it: the name, the from and to dates (yyyy-mm-dd), whether it is closed, the
 *  due-total fixed when it was closed (null while open), and each person's due-total fixed then, by person id
 *  (empty while open), and each person's carried figure fixed then, by person id (empty while open, and for a
 *  person whose figure was not fixed), each person's opening balance pasted for it (added to their carried figure
 *  while open, and part of the figure fixed when closed), by person id, and whether it is soft-closed (its figures fixed as for a closed one), and its cap on each person's OPEN or PENDING claims (null: none), and whether it is locked. */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> personTotals, boolean soft, Integer cap, Map<Long, String> personCarried,
        Map<Long, String> personOpenings, boolean locked) {
    public static PeriodResponse from(Period p, Map<Long, BigDecimal> personTotals) {
        return from(p, personTotals, null);
    }

    public static PeriodResponse from(Period p, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried) {
        return from(p, personTotals, personCarried, null);
    }

    public static PeriodResponse from(Period p, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried,
            Map<Long, BigDecimal> personOpenings) {
        Map<Long, String> shown = new TreeMap<>();
        if ((p.closed() || p.soft()) && personTotals != null) {
            personTotals.forEach((id, total) -> shown.put(id, total.toPlainString()));
        }
        Map<Long, String> carried = new TreeMap<>();
        if ((p.closed() || p.soft()) && personCarried != null) {
            personCarried.forEach((id, figure) -> carried.put(id, figure.toPlainString()));
        }
        Map<Long, String> openings = new TreeMap<>();
        if (personOpenings != null) {
            personOpenings.forEach((id, amount) -> openings.put(id, amount.toPlainString()));
        }
        return new PeriodResponse(p.name(), p.from().toString(), p.to().toString(), p.closed(),
                p.closedTotal() != null ? p.closedTotal().toPlainString() : null, shown, p.soft(), p.cap(), carried,
                openings, false);
    }

    /** This period as shown, locked or not. */
    public PeriodResponse locked(boolean locked) {
        return new PeriodResponse(name, from, to, closed, closedTotal, personTotals, soft, cap, personCarried,
                personOpenings, locked);
    }
}
