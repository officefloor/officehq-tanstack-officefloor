package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the people on the sheet the user watches. */
public class RespondWithWatched {
    public void respond(@Val SheetResponse sheet, @PathVariable("user") String user, @Val ViewAsOf asOf,
            SheetHistory history, ObjectResponse<List<Long>> response) {
        response.send(history.view(sheet.id(), asOf.on(),
                jdbc -> new WatchRepository(jdbc).watchedBy(sheet.id(), user == null ? "" : user.trim())));
    }
}
