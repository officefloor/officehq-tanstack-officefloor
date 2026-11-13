package net.officefloor.hq.app.watch;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatches {
    public void respond(@Val List<Watch> watches, ObjectResponse<List<WatchResponse>> response) {
        response.send(watches.stream().map(WatchResponse::from).toList());
    }
}
