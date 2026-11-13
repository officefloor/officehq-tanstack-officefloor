package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the role just given; runs only once it has passed every rule. */
public class AuditRoleSet {
    public void audit(@Val UserRole role, @RequestParam("user") String user, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + user + " user=" + role.user()
                + " role=" + role.role().label());
    }
}
