package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes APPROVER_SET to the audit file once the approver is stored. */
public class RecordApproverSet {
    public void record(@Val Person person, @Val ApproverChange change, Audit audit) {
        audit.record("APPROVER_SET sheet=" + person.sheetId() + " by=" + change.by() + " name=" + person.name()
                + " user=" + change.user());
    }
}
