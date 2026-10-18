package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's settings. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        response.send(new SettingsResponse(repository.findBySheetIdAndKey(sheetId.trim(), Setting.CLERK_LIMIT)
                .map(Setting::getValue).orElse(null)));
    }
}
