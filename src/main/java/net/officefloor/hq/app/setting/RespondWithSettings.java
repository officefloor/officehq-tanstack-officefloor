package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<SettingsResponse> response) {
        String sheet = sheetId.trim();
        response.send(new SettingsResponse(settings.get(sheet, Settings.CLERK_LIMIT),
                settings.floor(sheet).setScale(2).toPlainString()));
    }
}
