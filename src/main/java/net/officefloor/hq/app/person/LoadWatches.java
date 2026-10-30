package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The ids of the people the user watches on the sheet. */
public class LoadWatches {
    public void service(@PathVariable("sheetId") String sheetId, @PathVariable("user") String user, Watches watches,
            ObjectResponse<List<String>> response) {
        response.send(watches.watchedBy(sheetId, user));
    }
}
