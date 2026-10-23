package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rate for each grade that has one (as they stood at the end of the as-of date, when one is given),
 *  then its lineage's rates (its parents', then those of the sheet it was cloned from and so on), nearest first, each marked with its level and carrying its older versions. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        List<String> sheets = new ArrayList<>();
        sheets.add(id);
        sheets.addAll(repository.lineage(id));
        List<RateResponse> shown = new ArrayList<>();
        for (int level = 0; level < sheets.size(); level++) {
            List<Rate> versions = end != null ? repository.versionsAsOf(sheets.get(level), end)
                    : repository.versions(sheets.get(level));
            for (Rate r : Rates.forClaim(versions, 0)) {
                List<Rate> older = new ArrayList<>(versions.stream().filter(v -> v != r && v.grade().equals(r.grade())
                        && Objects.equals(v.from(), r.from())).toList());
                Collections.reverse(older);
                shown.add(RateResponse.from(r, level, older));
            }
        }
        response.send(shown);
    }
}
