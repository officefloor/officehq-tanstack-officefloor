package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Periods periods, AsOf asOfSheet,
            ObjectResponse<List<PeriodResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, () -> periods.onSheet(sheetId).stream()
                .map(p -> PeriodResponse.from(p, periods.personTotals(p))).toList()));
    }
}
