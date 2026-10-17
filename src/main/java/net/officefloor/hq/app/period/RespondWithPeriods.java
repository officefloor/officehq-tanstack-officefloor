package net.officefloor.hq.app.period;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's periods, by from-date. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodRepository repository,
            ObjectResponse<PeriodsResponse> response) {
        response.send(PeriodsResponse.from(repository.findBySheetIdOrdered(sheetId.trim())));
    }
}
