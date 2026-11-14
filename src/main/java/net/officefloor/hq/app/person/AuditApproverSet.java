package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just given an approver and the approver's user name. */
public class AuditApproverSet {
    public void audit(@Val Person person, @Val Approver approver, @RequestParam("user") String user, Audit audit) {
        audit.record("APPROVER_SET sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " user=" + approver.user());
    }
}
