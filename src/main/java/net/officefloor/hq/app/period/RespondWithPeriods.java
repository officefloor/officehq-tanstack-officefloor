package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's periods, earliest first (as they stood at the end of the as-of date, when one is given). */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        var openings = repository.personOpenings(id);
        if (end != null) {
            response.send(repository.listAsOf(id, end).stream()
                    .map(p -> PeriodResponse.from(p.period(), p.personTotals(), p.personCarried(),
                            openings.get(p.period().name()))).toList());
            return;
        }
        var totals = repository.personTotals(id);
        var carried = repository.personCarried(id);
        response.send(repository.list(id).stream()
                .map(p -> PeriodResponse.from(p, totals.get(p.name()), carried.get(p.name()),
                        openings.get(p.name()))).toList());
    }
}
