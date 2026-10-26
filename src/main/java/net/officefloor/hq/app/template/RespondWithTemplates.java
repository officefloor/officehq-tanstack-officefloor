package net.officefloor.hq.app.template;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithTemplates {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<TemplateResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> new TemplateRepository(jdbc).list(sheet.id()).stream()
                .map(TemplateResponse::of).toList()));
    }
}
