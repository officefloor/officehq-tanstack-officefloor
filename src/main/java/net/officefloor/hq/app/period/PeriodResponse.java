package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** A period as the screen sees it: its dates as yyyy-mm-dd, whether it is closed and, when it is,
 *  the due-total fixed when it was closed and each person's (`closedPersonTotals`, by person id;
 *  a person not in it shows 0.00). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> closedPersonTotals) {
    public static PeriodResponse from(Period period) {
        return from(period, Map.of());
    }

    public static PeriodResponse from(Period period, Map<Long, BigDecimal> personTotals) {
        Map<String, String> totals = new LinkedHashMap<>();
        personTotals.forEach((id, total) -> totals.put(id.toString(), total.setScale(2).toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().setScale(2).toPlainString(), totals);
    }
}
