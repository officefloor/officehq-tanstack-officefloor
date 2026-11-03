package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithOpenings {
    public void respond(@Val OpeningResult result, ObjectResponse<OpeningResult> response) {
        response.send(result);
    }
}
