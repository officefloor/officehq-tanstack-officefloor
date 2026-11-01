package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Responds with the ids of the people the signed-in user watches on the sheet. */
public class RespondWithWatches {
    public void respond(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Watches watches,
            ObjectResponse<List<Long>> response) {
        response.send(watches.watchedBy(sheet, user));
    }
}
