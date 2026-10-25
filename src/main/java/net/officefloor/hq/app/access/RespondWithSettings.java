package net.officefloor.hq.app.access;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<SettingResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> new SettingRepository(jdbc).listOn(sheet.id(),
                new SheetRepository(jdbc).today(sheet.id()))));
    }
}
