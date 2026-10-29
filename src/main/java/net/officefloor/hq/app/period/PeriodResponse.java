package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A period as the screen sees it: the dates as yyyy-mm-dd, the due-total fixed when closed (null while open), and
 * each person's due-total for it fixed then, by person id (empty while open; a person added later has none), and
 * each person's carried figure for it fixed then the same way (a period fixed before carried figures were has none), and
 * whether it is soft-closed (its figures fixed so, its claims still open to change), and its claim cap (null: none), and
 * each person's opening balance for it, by person id (none: 0.00), part of their carried figure, and whether it is locked.
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> personTotals, Map<Long, String> personCarried, boolean soft, Integer cap,
        Map<Long, String> personOpenings, boolean locked) {

    public static PeriodResponse from(Period period) {
        return from(period, Map.of());
    }

    public static PeriodResponse from(Period period, Map<Long, BigDecimal> openings) {
        Map<Long, String> totals = new LinkedHashMap<>();
        period.personTotals().forEach((id, total) -> totals.put(id, total.toPlainString()));
        Map<Long, String> carried = new LinkedHashMap<>();
        period.personCarried().forEach((id, figure) -> carried.put(id, figure.toPlainString()));
        Map<Long, String> opening = new LinkedHashMap<>();
        openings.forEach((id, amount) -> opening.put(id, amount.toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString(), totals, carried, period.soft(), period.cap(),
                opening, period.locked());
    }
}
