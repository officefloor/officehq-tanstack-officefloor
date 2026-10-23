package net.officefloor.hq.app.flag;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithFlag {
    public void respond(@Val FlagResponse saved, ObjectResponse<FlagResponse> response) {
        response.send(saved);
    }
}
