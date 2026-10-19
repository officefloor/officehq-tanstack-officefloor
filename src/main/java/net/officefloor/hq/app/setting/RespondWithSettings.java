package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every known setting of the sheet, blank ones included. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(List.of(new SettingResponse(ClerkLimit.KEY, repository.find(sheetId, ClerkLimit.KEY).orElse(""))));
    }
}
