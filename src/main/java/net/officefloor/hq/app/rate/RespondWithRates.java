package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.statement.SheetAsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rates, by grade, then those of its parent, the parent's parent, up to three levels up (each with its
 *  level), which apply to a grade on a date the sheet has no rate for; read as of a date (see AsOf), the sheet's own
 *  as they stood then (see SheetAsOf) and those inherited as they are now (a parent keeps its own today, so the date
 *  says nothing of when its rates were stored). */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, RateRepository rates,
            SettingRepository settings, ObjectResponse<List<RateResponse>> response) {
        LocalDate date = AsOf.parse(asOf);
        List<RateResponse> shown = new ArrayList<>();
        List<String> chain = rates.chain(sheetId.trim());
        if (date != null) {
            SheetAsOf sheet = SheetAsOf.read(chain.get(0), date, rates, settings);
            for (Rate rate : sheet.rates()) {
                shown.add(RateResponse.from(rate, 0, sheet.older(rate)));
            }
        }
        for (int level = date == null ? 0 : 1; level < chain.size(); level++) {
            for (Rate rate : rates.list(chain.get(level))) {
                shown.add(RateResponse.from(rate, level, rates.older(rate.sheetId(), rate.grade(), rate.from())));
            }
        }
        response.send(shown);
    }
}
