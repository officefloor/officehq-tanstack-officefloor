package net.officefloor.hq.app.watch;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatch {
    public void respond(@Val WatchResponse saved, ObjectResponse<WatchResponse> response) {
        response.send(saved);
    }
}
