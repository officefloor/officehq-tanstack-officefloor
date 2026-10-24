package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people on the sheet the user watches. */
public class RespondWithWatched {
    public void respond(@Val Sheet sheet, @RequestParam(name = "user", required = false) String user,
            WatchRepository watches, ObjectResponse<List<Long>> response) {
        response.send(watches.findBySheetIdAndUserNameOrderByIdAsc(sheet.getId(), AuditUser.decode(user)).stream()
                .map(Watch::getPersonId).toList());
    }
}
