package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings, Sheets sheets,
            ObjectResponse<SettingsResponse> response) {
        response.send(SettingsResponse.of(sheetId, sheets.today(sheetId), settings));
    }
}
