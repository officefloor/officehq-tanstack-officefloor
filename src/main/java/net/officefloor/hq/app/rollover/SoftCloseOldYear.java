package net.officefloor.hq.app.rollover;

import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.period.PeriodGenerateInput;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Soft-close each period of the old fiscal year that is still open (neither closed nor soft-closed), in start-date
 *  order, as a soft close does, writing PERIOD_SOFT_CLOSED period=<name> total=<fixed> for each. */
public class SoftCloseOldYear {
    public void softClose(@PathVariable("sheetId") String sheetId, @Val PeriodGenerateInput input,
            @Val OldYear oldYear, PeriodRepository periods, ClaimRepository claims, ClaimLogRepository logs,
            PersonRepository people, RateRepository rates, SettingRepository settings, SheetRepository sheets,
            PoolRepository pools, CrewRepository crews, Audit audit) throws InvalidPeriodException {
        String id = sheetId.trim();
        var names = oldYear.periods().stream().sorted(Comparator.comparing(Period::from)).map(Period::name).toList();
        for (String name : names) {
            Period period = periods.find(id, name);
            if (period == null || period.closed() || period.soft()) {
                continue;
            }
            Period closed = PeriodCloser.fix(id, period, true, periods, claims, logs, people, rates, settings, sheets,
                    pools, crews);
            audit.record("PERIOD_SOFT_CLOSED sheet=" + id + " by=" + input.user() + " period=" + closed.name()
                    + " total=" + closed.closedTotal().toPlainString());
        }
    }
}
