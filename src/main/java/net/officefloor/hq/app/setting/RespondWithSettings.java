package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<SettingsResponse> response) {
        response.send(new SettingsResponse(settings.get(sheetId.trim(), Settings.CLERK_LIMIT)));
    }
}
