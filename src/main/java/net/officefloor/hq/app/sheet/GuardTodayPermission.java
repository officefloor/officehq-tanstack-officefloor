package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Refuses a change of the sheet's today, before anything is written, by a role the sheet's permission matrix does
 *  not allow to change it (anyone but a viewer, by the standing rule). */
public class GuardTodayPermission {
    public void guard(@Val Sheet sheet, @RequestHeader(name = "X-User", required = false) String user,
            RoleRepository roles) throws InvalidTodayException {
        String role = roles.roleOf(sheet.getId(), AuditUser.decode(user));
        if (!sheet.allows("today", role)) {
            throw InvalidTodayException.refused(role);
        }
    }
}
