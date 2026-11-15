package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people the signed-in user watches on the sheet. */
public class ListWatches {
    public void list(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, WatchRepository watches,
            ObjectResponse<List<Long>> response) {
        response.send(watches.watchedBy(sheetId.trim(), by == null ? "" : by));
    }
}
