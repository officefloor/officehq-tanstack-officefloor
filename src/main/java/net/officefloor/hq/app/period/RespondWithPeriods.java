package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's periods; as of a date (asOf), as they stood at the end of it. */
public class RespondWithPeriods {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidTodayException {
        response.send(asOf.periods(sheet.getId(), AsOf.dateOf(asOfParam)).stream().map(PeriodResponse::from).toList());
    }
}
