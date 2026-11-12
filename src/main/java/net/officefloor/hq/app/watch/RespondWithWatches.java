package net.officefloor.hq.app.watch;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people the user watches on the sheet (none for no user); as of a date, those watched at its end. */
public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "user", required = false) String user,
            @RequestParam(name = "asOf", required = false) String asOf, WatchStore watches,
            ObjectResponse<List<Long>> response) {
        LocalDate on = History.date(asOf);
        response.send(user == null || user.isBlank() ? List.of()
                : on == null ? watches.watchedBy(sheetId.trim(), user.trim())
                : watches.watchedByAsOf(sheetId.trim(), user.trim(), on));
    }
}
