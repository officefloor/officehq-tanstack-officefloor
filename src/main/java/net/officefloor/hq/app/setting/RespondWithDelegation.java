package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithDelegation {
    public void respond(@Val Delegation delegation, ObjectResponse<Delegation> response) {
        response.send(delegation);
    }
}
