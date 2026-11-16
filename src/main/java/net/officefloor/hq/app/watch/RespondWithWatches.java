package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every watch on the sheet, in the order they were started. */
public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId, WatchRepository watches,
            ObjectResponse<List<Watch>> response) {
        response.send(watches.list(sheetId.trim()));
    }
}
