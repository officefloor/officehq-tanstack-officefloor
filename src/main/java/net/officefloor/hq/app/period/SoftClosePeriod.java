package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Soft-closes an open period, by the roles the sheet allows to close (see {@link PeriodCloser#softClose}). */
public class SoftClosePeriod {
    public void softClose(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods,
            Settings settings, PeriodCloser closer, ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!settings.allows(sheetId, request.user(), "close")) {
            throw new InvalidPeriodException("Your role may not close a period");
        }
        PeriodResponse period = periods.lock(sheetId, name);
        if (period == null) {
            throw new InvalidPeriodException("No period named " + name + " is on the sheet");
        }
        if (period.closed() || period.softClosed()) {
            throw new InvalidPeriodException("The period " + name + " is already closed or soft-closed");
        }
        closer.softClose(sheetId, period, request.user());
        response.send(periods.list(sheetId));
    }
}
