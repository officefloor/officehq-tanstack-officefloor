package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListSettings {
    public void list(@PathVariable("sheetId") String sheetId, SettingRepository settings,
            ObjectResponse<List<Setting>> response) {
        response.send(settings.list(sheetId.trim()));
    }
}
