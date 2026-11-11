package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.setting.Settings;

/**
 * A period, as the screen reads it: its name, its from and to dates (yyyy-MM-dd), whether it is
 * closed, the due-total fixed when it was closed ("" while open), and each person's due-total fixed
 * when it was closed, by person id (empty while open), and whether it is soft-closed (closed, its
 * claims still open to change), and the fiscal year its from-date falls in (FY<yyyy>), and its cap
 * ("" for none), and each person's carried figure fixed when it was closed, by person id (empty while open),
 * and each person's opening balance for it, by person id, and whether it is locked, and the order it
 * was added in ({@code added}: a larger one added later).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, boolean soft, String fy, String cap, Map<String, String> personCarried,
        Map<String, String> personOpenings, boolean locked, long added) {
    public static PeriodResponse from(Period period, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried,
            Map<Long, BigDecimal> personOpenings, Settings settings, long added) {
        Map<String, String> totals = new LinkedHashMap<>();
        personTotals.forEach((id, total) -> totals.put(String.valueOf(id), total.setScale(2).toPlainString()));
        Map<String, String> carried = new LinkedHashMap<>();
        personCarried.forEach((id, figure) -> carried.put(String.valueOf(id), figure.setScale(2).toPlainString()));
        Map<String, String> openings = new LinkedHashMap<>();
        personOpenings.forEach((id, amount) -> openings.put(String.valueOf(id), amount.setScale(2).toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? "" : period.closedTotal().setScale(2).toPlainString(),
                totals, period.soft(), settings.fiscalYearOf(period.from()),
                period.cap() == null ? "" : String.valueOf(period.cap()), carried, openings, period.locked(), added);
    }
}
