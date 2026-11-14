package net.officefloor.hq.app.period;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.opening.OpeningRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val List<Period> periods, @Val Sheet sheet, OpeningRepository openings, PeriodRepository repository,
            ObjectResponse<List<PeriodResponse>> response) {
        var totals = openings.totals(sheet.id());
        var added = repository.addedOrder(sheet.id());
        response.send(periods.stream()
                .map((p) -> PeriodResponse.from(p, totals.getOrDefault(p.name(), Map.of()), added.get(p.name())))
                .toList());
    }
}
