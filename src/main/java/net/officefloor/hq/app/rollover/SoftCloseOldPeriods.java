package net.officefloor.hq.app.rollover;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Soft-closes every period of the old fiscal year still open, earliest start first, each as a soft close would: its
 * figures fixed as they stand now (see {@link PeriodCloser}), its pool cuts fixed, and recorded as PERIOD_SOFT_CLOSED.
 */
public class SoftCloseOldPeriods {
    public void softClose(@Val Rollover rollover, @Val Sheet sheet, @RequestParam("user") String user,
            PeriodRepository periods, PeriodCloser closer, PoolCuts poolCuts, Audit audit) {
        Set<String> old = rollover.oldPeriods().stream().map(Period::name).collect(Collectors.toSet());
        periods.findAll(sheet.id()).stream().filter((p) -> old.contains(p.name()) && !p.fixed())
                .sorted(Comparator.comparing(Period::from).thenComparing(Period::to)).forEach((open) -> {
                    Period closed = closer.softClose(open);
                    periods.saveClosed(closed);
                    poolCuts.fix(closed.sheetId(), closed.name());
                    audit.record("PERIOD_SOFT_CLOSED sheet=" + closed.sheetId() + " by=" + user + " period="
                            + closed.name() + " total=" + closed.closedTotal().setScale(2).toPlainString());
                });
    }
}
