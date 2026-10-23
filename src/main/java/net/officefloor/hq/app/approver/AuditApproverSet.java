package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record the person's new approver in the audit file. */
public class AuditApproverSet {
    public void audit(@Val ApproverInput input, @Val Person loaded, Audit audit) {
        audit.record("APPROVER_SET sheet=" + loaded.sheetId() + " by=" + input.user() + " name=" + loaded.name()
                + " user=" + input.approver());
    }
}
