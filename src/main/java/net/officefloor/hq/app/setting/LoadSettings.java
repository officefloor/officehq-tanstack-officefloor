package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings; with an as-of date, as they stood at the end of it. */
public class LoadSettings {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Settings settings, AsOfView view,
            ObjectResponse<SettingsResponse> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? settings.load(sheetId) : view.settings(sheetId, date));
    }
}
