package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePeriod {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPeriod period, Periods periods, Audit audit,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException {
        periods.add(sheetId, period);
        audit.recordOnCommit("PERIOD_ADDED sheet=" + sheetId + " by=" + period.user()
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to()
                + (period.cap() == null ? "" : " cap=" + period.cap()));
        response.send(periods.list(sheetId));
    }
}
