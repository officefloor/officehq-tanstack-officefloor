package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Soft-closes every period of the old fiscal year that is still open (neither closed nor soft-closed), in
 *  start-date order, each as a soft close would, recording PERIOD_SOFT_CLOSED sheet=<id> by=<user> period=<name>
 *  total=<0.00> for each. Runs after the ROLLOVER record. */
public class SoftCloseRollover {
    public void softClose(@PathVariable("sheetId") String sheetId, @Val RolloverInput rollover, PeriodStore store,
            RollupService rollups, PoolCuts cuts, Audit audit) throws InvalidSheetException {
        String sheet = sheetId.trim();
        List<String> names = rollover.periods().stream()
                .sorted(Comparator.comparing((PeriodResponse p) -> LocalDate.parse(p.from()))
                        .thenComparing(PeriodResponse::name))
                .map(PeriodResponse::name).toList();
        for (String name : names) {
            PeriodResponse period = store.find(sheet, name);
            if (period == null || period.closed() || period.soft()) {
                continue;
            }
            PeriodResponse closed = PeriodClosing.fixSoft(sheet, period, store, rollups, cuts);
            audit.record("PERIOD_SOFT_CLOSED sheet=" + sheet + " by=" + rollover.user() + " period=" + closed.name()
                    + " total=" + closed.total());
        }
    }
}
