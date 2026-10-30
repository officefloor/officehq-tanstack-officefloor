package net.officefloor.hq.app.period;

import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class SoftCloseRolloverYear {
    // After the ROLLOVER line, every period of the old year still open is soft-closed in start-date order, each as a
    // soft close would: its figures fixed as they stand now, its pool cuts fixed, and PERIOD_SOFT_CLOSED recorded.
    public void softClose(@Val Rollover rollover, Periods periods, Rollup rollup, PoolCuts poolCuts,
            ServerHttpConnection connection, Audit audit) {
        String user = Audit.user(connection);
        for (Period old : rollover.old().stream().sorted(Comparator.comparing(Period::from)).toList()) {
            Period period = periods.find(old.sheetId(), old.name());
            if (period == null || period.closed() || period.soft()) {
                continue;
            }
            Period soft = period.softClosedAt(rollup.personDueTotals(period.sheetId(), period.from(), period.to()),
                    rollup.personCarried(period.sheetId(), period, periods.list(period.sheetId())));
            if (!periods.softClose(soft)) {
                continue;
            }
            poolCuts.fix(soft.sheetId(), soft.name());
            audit.record(Audit.line("PERIOD_SOFT_CLOSED", soft.sheetId(), user,
                    "period=" + soft.name(), "total=" + soft.closedTotal().toPlainString()));
        }
    }
}
