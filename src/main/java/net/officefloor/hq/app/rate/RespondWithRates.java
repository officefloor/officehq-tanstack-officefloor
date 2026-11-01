package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, Rates rates, AsOf asOfSheet,
            ObjectResponse<List<RateResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, preview,
                () -> rates.onSheet(sheetId).stream().map(RateResponse::from).toList()));
    }
}
