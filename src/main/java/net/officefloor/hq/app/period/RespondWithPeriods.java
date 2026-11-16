package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's periods, by from-date. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodRepository periods,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(periods.list(sheetId.trim()).stream().map(PeriodResponse::from).toList());
    }
}
