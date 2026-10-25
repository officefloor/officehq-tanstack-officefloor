package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's today as the signed-in user's screen shows it: an as-of date stands in for it. */
public class RespondWithSheetTodayAsOf {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<SheetTodayResponse> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> {
            SheetRepository sheets = new SheetRepository(jdbc);
            return new SheetTodayResponse(sheet.id(), sheets.today(sheet.id()).toString(), sheets.isArchived(sheet.id()));
        }));
    }
}
