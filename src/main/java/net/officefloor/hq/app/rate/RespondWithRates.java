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
 *  as they stood then (see SheetAsOf). */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, RateRepository rates,
            SettingRepository settings, ObjectResponse<List<RateResponse>> response) {
        LocalDate date = AsOf.parse(asOf);
        if (date != null) {
            response.send(SheetAsOf.read(sheetId.trim(), date, rates, settings).rates().stream()
                    .map(RateResponse::from).toList());
            return;
        }
        List<RateResponse> shown = new ArrayList<>();
        List<String> chain = rates.chain(sheetId.trim());
        for (int level = 0; level < chain.size(); level++) {
            for (Rate rate : rates.list(chain.get(level))) {
                shown.add(RateResponse.from(rate, level));
            }
        }
        response.send(shown);
    }
}
