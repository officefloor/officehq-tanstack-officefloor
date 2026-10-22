package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's settings as they are now. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        response.send(SettingsResponse.from(repository.of(sheetId.trim())));
    }
}
