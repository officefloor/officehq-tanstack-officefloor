package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every period on the sheet, by from-date. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(repository.findBySheet(sheetId).stream().map(PeriodResponse::from).toList());
    }
}
