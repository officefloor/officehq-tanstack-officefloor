package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records an approver: APPROVER_SET sheet=<id> by=<user> name=<name> user=<approver>. */
public class AuditApproverSet {
    public void audit(@Val ApproverInput input, @Val Person person, Audit audit) {
        audit.record("APPROVER_SET sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " user=" + input.approver());
    }
}
