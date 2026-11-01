package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** A period as the screen sees it: its dates as yyyy-mm-dd, whether it is closed and, when it is,
 *  the due-total fixed when it was closed and each person's (`closedPersonTotals`, by person id;
 *  a person not in it shows 0.00) and each person's carried figure (`closedPersonCarried`, alike), whether the close is a soft close, and its cap (null for none),
 *  and each person's opening balance for it (`openings`, by person id; a person with none is absent), and whether it is locked. */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> closedPersonTotals, Map<String, String> closedPersonCarried, boolean soft, Integer cap,
        Map<String, String> openings, boolean locked) {
    public static PeriodResponse from(Period period) {
        return from(period, Map.of(), Map.of(), Map.of());
    }

    public static PeriodResponse from(Period period, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried) {
        return from(period, personTotals, personCarried, Map.of());
    }

    public static PeriodResponse from(Period period, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, Map<Long, BigDecimal> personOpenings) {
        Map<String, String> totals = new LinkedHashMap<>();
        personTotals.forEach((id, total) -> totals.put(id.toString(), total.setScale(2).toPlainString()));
        Map<String, String> carried = new LinkedHashMap<>();
        personCarried.forEach((id, figure) -> carried.put(id.toString(), figure.setScale(2).toPlainString()));
        Map<String, String> openings = new LinkedHashMap<>();
        personOpenings.forEach((id, amount) -> openings.put(id.toString(), amount.setScale(2).toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().setScale(2).toPlainString(), totals, carried,
                period.soft(), period.cap(), openings, period.locked());
    }
}
