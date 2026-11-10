package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's periods, by from-date. Read as of a date, as they stood then: a period closed then
 * shows the people's figures fixed by the close it is still in now (none when it has since been
 * reopened or closed again with another due-total).
 */
public class RespondWithPeriods {
    public void respond(@Val Sheet sheet, PeriodRepository repository, HttpServletRequest request,
            ObjectResponse<PeriodsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        Map<String, Map<Long, BigDecimal>> personTotals = repository.personTotals(sheet.id());
        List<Period> periods = repository.findAll(sheet.id());
        if (asOf != null) {
            Map<String, Period> now = new HashMap<>();
            periods.forEach((p) -> now.put(p.name(), p));
            periods = repository.findAllAsOf(sheet.id(), asOf);
            Map<String, Map<Long, BigDecimal>> thenTotals = new HashMap<>();
            for (Period p : periods) {
                Period current = now.get(p.name());
                if (p.closed() && current != null && current.closed() && same(p.closedTotal(), current.closedTotal())) {
                    thenTotals.put(p.name(), personTotals.getOrDefault(p.name(), Map.of()));
                }
            }
            personTotals = thenTotals;
        }
        Map<String, Map<Long, BigDecimal>> totals = personTotals;
        response.send(new PeriodsResponse(periods.stream()
                .map(p -> PeriodResponse.from(p, totals.getOrDefault(p.name(), Map.of()))).toList()));
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
