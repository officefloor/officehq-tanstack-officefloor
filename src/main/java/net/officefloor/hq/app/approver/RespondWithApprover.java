package net.officefloor.hq.app.approver;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithApprover {
    public void respond(@Val ApproverResponse saved, ObjectResponse<ApproverResponse> response) {
        response.send(saved);
    }
}
