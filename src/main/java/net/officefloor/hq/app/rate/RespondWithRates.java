package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rate for each grade that has one (as they stood at the end of the as-of date, when one is given). */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        List<Rate> rates = end != null ? repository.listAsOf(id, end) : repository.list(id);
        response.send(rates.stream().map(RateResponse::from).toList());
    }
}
