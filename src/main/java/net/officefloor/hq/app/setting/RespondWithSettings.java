package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's settings. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheet") String sheet, Settings settings,
            ObjectResponse<SettingsResponse> response) {
        response.send(settings.all(sheet));
    }
}
