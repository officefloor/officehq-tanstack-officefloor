package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's settings, by key. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<Setting>> response) {
        response.send(settings.list(sheetId.trim()));
    }
}
