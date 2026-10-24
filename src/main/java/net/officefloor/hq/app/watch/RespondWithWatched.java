package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people on the sheet the user watches; as of a date (asOf), as they stood at the end of it. */
public class RespondWithWatched {
    public void respond(@Val Sheet sheet, @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<Long>> response) throws InvalidTodayException {
        response.send(asOf.watches(sheet.getId(), AuditUser.decode(user), AsOf.dateOf(asOfParam)).stream()
                .map(Watch::getPersonId).toList());
    }
}
