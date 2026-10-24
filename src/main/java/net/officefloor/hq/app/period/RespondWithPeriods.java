package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val Sheet sheet, PeriodRepository repository, ObjectResponse<List<PeriodResponse>> response) {
        response.send(repository.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()).stream().map(PeriodResponse::from).toList());
    }
}
