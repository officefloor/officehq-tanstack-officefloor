package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPasteClaims {
    public void respond(@Val PasteClaimsResult result, ObjectResponse<PasteClaimsResult> response) {
        response.send(result);
    }
}
