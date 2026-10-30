package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId, Periods periods,
            ObjectResponse<List<PeriodResponse>> response) {
        // Each with its people's opening balances and the order it was added in.
        String sheet = sheetId.trim();
        Map<String, Map<Long, BigDecimal>> openings = periods.openings(sheet);
        Map<String, Long> added = periods.added(sheet);
        response.send(periods.list(sheet).stream()
                .map(p -> PeriodResponse.from(p, openings.getOrDefault(p.name(), Map.of()), added.get(p.name()))).toList());
    }
}
