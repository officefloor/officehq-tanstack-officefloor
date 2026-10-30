package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Locks a period, by a steward only: a locked period cannot be reopened until it is unlocked. */
public class LockPeriod {
    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods, Settings settings, Audit audit,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidPeriodException("Only a steward may lock a period");
        }
        PeriodResponse period = periods.lock(sheetId, name);
        if (period == null) {
            throw new InvalidPeriodException("No period named " + name + " is on the sheet");
        }
        if (period.locked()) {
            throw new InvalidPeriodException("The period " + name + " is already locked");
        }
        periods.setLocked(sheetId, name, true);
        audit.recordOnCommit("PERIOD_LOCKED sheet=" + sheetId + " by=" + request.user() + " period=" + name);
        response.send(periods.list(sheetId));
    }
}
