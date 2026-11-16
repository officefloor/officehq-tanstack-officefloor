package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.statement.SheetAsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings, by key; read as of a date (see AsOf), as they stood then (see SheetAsOf). */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, SettingRepository settings,
            RateRepository rates, ObjectResponse<List<Setting>> response) {
        LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? settings.list(sheetId.trim())
                : SheetAsOf.read(sheetId.trim(), date, rates, settings).settings());
    }
}
