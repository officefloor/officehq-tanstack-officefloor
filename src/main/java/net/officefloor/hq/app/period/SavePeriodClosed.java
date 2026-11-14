package net.officefloor.hq.app.period;

import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.plugin.variable.Val;

/** Stores the period's closed state; a closed or soft-closed one fixes its pool cuts as they stand now, a reopened one makes them live. */
public class SavePeriodClosed {
    public void save(@Val Period period, PeriodRepository repository, PoolCuts poolCuts) {
        repository.saveClosed(period);
        if (period.fixed()) {
            poolCuts.fix(period.sheetId(), period.name());
        } else {
            poolCuts.unfix(period.sheetId(), period.name());
        }
    }
}
