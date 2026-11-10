package net.officefloor.hq.app.watch;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The people the signed-in user watches on the sheet. */
public class RespondWithWatches {
    public void respond(@Val Sheet sheet, WatchRepository watches, HttpServletRequest request,
            ObjectResponse<WatchesResponse> response) {
        response.send(new WatchesResponse(
                watches.watchedBy(sheet.id(), Audit.user(request)).stream().map(String::valueOf).toList()));
    }
}
