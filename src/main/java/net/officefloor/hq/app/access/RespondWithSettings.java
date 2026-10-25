package net.officefloor.hq.app.access;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val SheetResponse sheet, SettingRepository repository, SheetRepository sheets,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(repository.listOn(sheet.id(), sheets.today(sheet.id())));
    }
}
