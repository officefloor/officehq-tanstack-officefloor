package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the sheet's descendants (see SettingRepository#descendants). */
public class RespondWithDescendants {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<String>> response) {
        response.send(settings.descendants(sheetId.trim()));
    }
}
