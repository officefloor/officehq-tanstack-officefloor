package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Val;

/** Runs after the approver is saved: APPROVER_SET with the approver's user name (empty: none). */
public class RecordApproverSet {
    public void record(@Val Person person, @Val Actor actor, Audit audit) {
        audit.record("APPROVER_SET sheet=" + person.getSheetId() + " by=" + actor.user() + " name=" + person.getName()
                + " user=" + (person.getApprover() == null ? "" : person.getApprover()));
    }
}
