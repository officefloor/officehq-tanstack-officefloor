package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<PeriodResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> {
            PeriodRepository periods = new PeriodRepository(jdbc);
            Map<Long, Map<Long, BigDecimal>> openings = periods.openings(sheet.id());
            return periods.list(sheet.id()).stream()
                    .map(p -> PeriodResponse.from(p, openings.getOrDefault(p.id(), Map.of()))).toList();
        }));
    }
}
