package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val SheetResponse sheet, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(PeriodResponse::from).toList());
    }
}
