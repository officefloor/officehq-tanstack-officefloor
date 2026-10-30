package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's today; with an as-of date, that date stands for it (and archived is as at the end of it). */
public class LoadToday {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Sheets sheets, AsOfView view,
            ObjectResponse<TodayResponse> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? TodayResponse.of(sheetId, sheets) : view.today(sheetId, date));
    }
}
