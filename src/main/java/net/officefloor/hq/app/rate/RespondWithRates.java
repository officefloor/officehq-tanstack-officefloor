package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rate for each grade that has one (as they stood at the end of the as-of date, when one is given),
 *  then its parents' rates, nearest first, each marked with its level. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        List<String> sheets = new ArrayList<>();
        sheets.add(id);
        sheets.addAll(repository.parents(id));
        List<RateResponse> shown = new ArrayList<>();
        for (int level = 0; level < sheets.size(); level++) {
            List<Rate> rates = end != null ? repository.listAsOf(sheets.get(level), end) : repository.list(sheets.get(level));
            for (Rate r : rates) {
                shown.add(RateResponse.from(r, level));
            }
        }
        response.send(shown);
    }
}
