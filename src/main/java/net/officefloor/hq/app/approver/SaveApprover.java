package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Make the user the loaded person's approver, replacing any they had (blank leaves them without one). */
public class SaveApprover {
    public void save(@Val ApproverInput input, @Val Person loaded, ApproverRepository approvers,
            Out<ApproverResponse> saved) {
        approvers.set(loaded.sheetId(), loaded.id(), input.approver());
        saved.set(new ApproverResponse(loaded.id(), input.approver()));
    }
}
