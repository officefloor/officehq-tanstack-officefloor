package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithWatches {
    public void respond(@PathVariable("sheetId") String sheetId, Watches watches,
            ObjectResponse<List<WatchResponse>> response) {
        response.send(watches.list(sheetId.trim()));
    }
}
