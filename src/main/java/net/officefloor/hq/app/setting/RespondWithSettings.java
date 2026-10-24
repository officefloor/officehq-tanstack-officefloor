package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings; as of a date (asOf), as they stood at the end of it. */
public class RespondWithSettings {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<SettingsResponse> response) throws InvalidTodayException {
        response.send(SettingsResponse.from(asOf.sheet(sheet, AsOf.dateOf(asOfParam))));
    }
}
