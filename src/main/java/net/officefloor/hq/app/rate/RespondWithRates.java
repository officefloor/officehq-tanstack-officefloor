package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.statement.SheetAsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rates, by grade; read as of a date (see AsOf), as they stood then (see SheetAsOf). */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, RateRepository rates,
            SettingRepository settings, ObjectResponse<List<RateResponse>> response) {
        LocalDate date = AsOf.parse(asOf);
        List<Rate> shown = date == null ? rates.list(sheetId.trim())
                : SheetAsOf.read(sheetId.trim(), date, rates, settings).rates();
        response.send(shown.stream().map(RateResponse::from).toList());
    }
}
