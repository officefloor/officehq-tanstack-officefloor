package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Unlocks a locked period, by a steward only, so it can be reopened again. */
public class UnlockPeriod {
    public void unlock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods, Settings settings, Audit audit,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidPeriodException("Only a steward may unlock a period");
        }
        PeriodResponse period = periods.lock(sheetId, name);
        if (period == null) {
            throw new InvalidPeriodException("No period named " + name + " is on the sheet");
        }
        if (!period.locked()) {
            throw new InvalidPeriodException("The period " + name + " is not locked");
        }
        periods.setLocked(sheetId, name, false);
        audit.recordOnCommit("PERIOD_UNLOCKED sheet=" + sheetId + " by=" + request.user() + " period=" + name);
        response.send(periods.list(sheetId));
    }
}
