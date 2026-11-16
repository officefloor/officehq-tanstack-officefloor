package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's periods, by from-date; read as of a date (see AsOf), as they stood then. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PeriodRepository periods,
            ObjectResponse<List<PeriodResponse>> response) {
        LocalDate date = AsOf.parse(asOf);
        response.send(date == null
                ? periods.list(sheetId.trim()).stream().map((p) -> PeriodResponse.from(p, periods)).toList()
                : periods.listAsOf(sheetId.trim(), date).entrySet().stream()
                        .map((e) -> PeriodResponse.from(e.getKey(), e.getValue(), periods.openings(e.getKey(), date))).toList());
    }
}
