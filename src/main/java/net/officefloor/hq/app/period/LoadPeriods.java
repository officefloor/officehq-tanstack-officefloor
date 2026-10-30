package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's periods; with an as-of date, as they stood at the end of it. */
public class LoadPeriods {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Periods periods, AsOfView view,
            ObjectResponse<List<PeriodResponse>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? periods.list(sheetId) : view.periods(sheetId, date));
    }
}
