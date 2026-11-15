package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithVoidReason {
    public void respond(@Val VoidReason reason, ObjectResponse<VoidReason> response) {
        response.send(reason);
    }
}
