package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSplit {
    public void respond(@Val Split split, ObjectResponse<Claim> response) {
        response.send(split.created());
    }
}
