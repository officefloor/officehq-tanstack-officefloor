package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rates; with an as-of date, as they stood at the end of it. */
public class LoadRates {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Rates rates, AsOfView view,
            ObjectResponse<List<RateResponse>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? rates.list(sheetId) : view.rates(sheetId, date));
    }
}
