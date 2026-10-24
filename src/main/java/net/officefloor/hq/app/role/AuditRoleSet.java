package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the user name's role, once it is saved. */
public class AuditRoleSet {
    public void audit(@Val Role role, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.getSheetId() + " by=" + AuditUser.decode(user)
                + " user=" + role.getUserName() + " role=" + role.getRole());
    }
}
