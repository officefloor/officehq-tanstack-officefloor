package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The rates of the sheet's parent, its parent's parent and so on (up to three levels up), nearest
 *  first; with an as-of date, of the ancestors as the sheet's parent chain stood at the end of it, each
 *  with its rates as they are now (an ancestor's history runs by its own today, not this sheet's). */
public class LoadInheritedRates {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Rates rates, AsOfView view,
            ObjectResponse<List<List<RateResponse>>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? rates.inherited(sheetId)
                : view.ancestors(sheetId, date).stream().map(rates::list).toList());
    }
}
