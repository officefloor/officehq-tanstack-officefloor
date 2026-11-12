package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithVoidReason {
    public void respond(@Val VoidReasonInput input, ObjectResponse<VoidReasonResponse> response) {
        response.send(new VoidReasonResponse(input.reason()));
    }
}
