package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the new claim the split made. */
public class RespondWithSplit {
    public void respond(@Val Split split, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(split.made()));
    }
}
