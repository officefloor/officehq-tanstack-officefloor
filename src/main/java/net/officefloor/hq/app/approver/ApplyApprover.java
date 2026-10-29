package net.officefloor.hq.app.approver;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class ApplyApprover {
    // Setting an approver replaces any the person had, and is recorded as APPROVER_SET.
    public void apply(@Val Approver approver, @Val Person person, Approvers approvers, ServerHttpConnection connection,
            Audit audit) {
        approvers.set(person.id(), approver.user());
        audit.record(Audit.line("APPROVER_SET", person.sheetId(), Audit.user(connection), "name=" + person.name(),
                "user=" + approver.user()));
    }
}
