package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDelegation {
    public void respond(@Val Delegation delegation, ObjectResponse<DelegationResponse> response) {
        response.send(DelegationResponse.from(delegation));
    }
}
