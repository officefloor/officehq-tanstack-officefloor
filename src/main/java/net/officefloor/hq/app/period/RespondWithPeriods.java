package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val Sheet sheet, PeriodRepository repository,
            ObjectResponse<PeriodsResponse> response) {
        Map<String, Map<Long, BigDecimal>> personTotals = repository.personTotals(sheet.id());
        response.send(new PeriodsResponse(repository.findAll(sheet.id()).stream()
                .map(p -> PeriodResponse.from(p, personTotals.getOrDefault(p.name(), Map.of()))).toList()));
    }
}
