package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val Sheet sheet, PeriodRepository repository,
            ObjectResponse<PeriodsResponse> response) {
        response.send(new PeriodsResponse(
                repository.findAll(sheet.id()).stream().map(PeriodResponse::from).toList()));
    }
}
