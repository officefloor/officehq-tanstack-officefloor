package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's periods, earliest first. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(PeriodResponse::from).toList());
    }
}
