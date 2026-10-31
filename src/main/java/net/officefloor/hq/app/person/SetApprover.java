package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Sets the person's approver, replacing any they had. */
public class SetApprover {
    public void set(@Val ValidApprover request, @Val Person person, People people, Audit audit) {
        people.setApprover(person, request.approver().isEmpty() ? null : request.approver());
        audit.recordOnCommit("APPROVER_SET sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " user=" + request.approver());
    }
}
