package net.officefloor.hq.app.role;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the assigned role. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditRoleSet {
    public void audit(@Val Sheet sheet, @Val Role role, HttpServletRequest request, Audit audit) {
        audit.record("ROLE_SET sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " user=" + role.user() + " role=" + role.role());
    }
}
