package net.officefloor.hq.app.watch;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The people the signed-in user watches on the sheet; read as of a date, those they watched by then. */
public class RespondWithWatches {
    public void respond(@Val Sheet sheet, WatchRepository watches, HttpServletRequest request,
            ObjectResponse<WatchesResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<Long> ids = asOf == null ? watches.watchedBy(sheet.id(), Audit.user(request))
                : watches.watchedBy(sheet.id(), Audit.user(request), asOf);
        response.send(new WatchesResponse(ids.stream().map(String::valueOf).toList()));
    }
}
