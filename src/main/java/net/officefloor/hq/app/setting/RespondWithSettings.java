package net.officefloor.hq.app.setting;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every known setting of the sheet, blank ones included; the floor and the terms show their defaults until it is set. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(List.of(new SettingResponse(ClerkLimit.KEY, repository.find(sheetId, ClerkLimit.KEY).orElse("")),
                new SettingResponse(Floor.KEY, repository.find(sheetId, Floor.KEY).orElse(Floor.DEFAULT)),
                new SettingResponse(Terms.KEY, repository.find(sheetId, Terms.KEY).orElse(Terms.DEFAULT))));
    }
}
