package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads who closes the period and loads it. */
public class LoadPeriod {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody PeriodCloseRequest request, Periods periods, Out<PeriodCloseRequest> body,
            Out<Period> periodOut) throws InvalidSheetRequestException {
        Period period = periods.find(sheetId, name);
        if (period == null) throw new InvalidSheetRequestException("No period named " + name);
        body.set(request == null ? new PeriodCloseRequest(null) : request);
        periodOut.set(period);
    }
}
