package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's settings. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        String id = sheetId.trim();
        response.send(new SettingsResponse(repository.findBySheetIdAndKey(id, Setting.CLERK_LIMIT)
                .map(Setting::getValue).orElse(null), repository.floor(id).setScale(2).toPlainString(),
                String.valueOf(repository.terms(id)),
                repository.latePct(id).setScale(2).toPlainString()));
    }
}
