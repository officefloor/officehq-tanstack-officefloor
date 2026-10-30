package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSettings {
    public void service(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<SettingsResponse> response) {
        response.send(settings.load(sheetId));
    }
}
