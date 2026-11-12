package net.officefloor.hq.app.setting;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store,
            ObjectResponse<SettingsResponse> response) {
        response.send(new SettingsResponse(store.find(sheetId.trim(), SettingStore.CLERK_LIMIT),
                store.floor(sheetId.trim()).setScale(2).toPlainString()));
    }
}
