package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, Periods periods,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(periods.onSheet(sheetId).stream().map(PeriodResponse::from).toList());
    }
}
