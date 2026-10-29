package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AutoClosePeriods {
    // Once the sheet's today is changed: with grace days set, every open period whose to-date plus the grace days is before the
    // new today is closed as a close by hand would (its people's figures and its pool members' cuts fixed as they stand now), in order of their to-dates,
    // each audited as PERIOD_CLOSED ending auto=yes. A soft-closed period keeps the figures it was fixed at.
    public void autoClose(@Val Sheet sheet, Periods periods, Rollup rollup, PoolCuts poolCuts, Settings settings,
            ServerHttpConnection connection, Audit audit) {
        String sheetId = sheet.id();
        Integer grace = settings.graceDays(sheetId);
        if (grace == null) {
            return;
        }
        LocalDate today = sheet.today();
        for (Period period : periods.list(sheetId).stream().sorted(Comparator.comparing(Period::to)).toList()) {
            if (period.closed() || period.soft() || !period.to().plusDays(grace).isBefore(today)) {
                continue;
            }
            Period closed = period.closedAt(rollup.personDueTotals(sheetId, period.from(), period.to()),
                rollup.personCarried(sheetId, period, periods.list(sheetId)));
            if (periods.close(closed)) {
                poolCuts.fix(sheetId, closed.name());
                audit.record(Audit.line("PERIOD_CLOSED", sheetId, Audit.user(connection), "period=" + closed.name(),
                        "total=" + closed.closedTotal().toPlainString(), "auto=yes"));
            }
        }
    }
}
