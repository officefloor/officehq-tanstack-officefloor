package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.sheet.AsOf;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every watch on the sheet, in the order they were started; read as of a date (see AsOf), as they stood then. */
public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, WatchRepository watches,
            ObjectResponse<List<Watch>> response) {
        java.time.LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? watches.list(sheetId.trim()) : watches.listAsOf(sheetId.trim(), date));
    }
}
