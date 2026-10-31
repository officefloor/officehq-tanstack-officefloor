package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the new claim split off. */
public class RespondWithClaimSplit {
    public void respond(@Val ClaimSplit split, Figures figures, ClaimLog log, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(split.into(), figures, log.of(split.into())));
    }
}
