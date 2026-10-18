package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records ROLE_SET once the role is saved; the signed-in user arrives as X-User. */
public class AuditRoleSet {
    public void audit(@Val UserRole role, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " user=" + role.getUser() + " role=" + role.getRole());
    }
}
