package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people the user watches on the sheet (none for no user). */
public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "user", required = false) String user, WatchStore watches,
            ObjectResponse<List<Long>> response) {
        response.send(user == null || user.isBlank() ? List.of() : watches.watchedBy(sheetId.trim(), user.trim()));
    }
}
