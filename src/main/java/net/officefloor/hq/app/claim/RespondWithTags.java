package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Every tag ever used on the sheet's claims, sorted. */
public class RespondWithTags {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<String>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> new ClaimRepository(jdbc).tagsUsed(sheet.id())));
    }
}
