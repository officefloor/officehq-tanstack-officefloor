package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reopens a closed period, by the roles the sheet's reopen permission allows (by default a steward only): its fixed due-total is dropped, so it totals its
 *  claims' current figures again. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods, Settings settings, Audit audit,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException {
        if (!settings.allows(sheetId, request.user(), "reopen")) {
            throw new InvalidPeriodException("Your role may not reopen a period");
        }
        PeriodResponse period = periods.lock(sheetId, name);
        if (period == null) {
            throw new InvalidPeriodException("No period named " + name + " is on the sheet");
        }
        if (!period.closed()) {
            throw new InvalidPeriodException("The period " + name + " is not closed");
        }
        periods.reopen(sheetId, name);
        audit.recordOnCommit("PERIOD_REOPENED sheet=" + sheetId + " by=" + request.user() + " period=" + name);
        response.send(periods.list(sheetId));
    }
}
