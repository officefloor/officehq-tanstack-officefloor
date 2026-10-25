package net.officefloor.hq.app.access;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val SheetResponse sheet, SettingRepository repository,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(SettingResponse::from).toList());
    }
}
