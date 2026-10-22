package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's periods, earliest first. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        String id = sheetId.trim();
        var totals = repository.personTotals(id);
        response.send(repository.list(id).stream().map(p -> PeriodResponse.from(p, totals.get(p.name()))).toList());
    }
}
