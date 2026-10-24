package net.officefloor.hq.app.role;

import net.officefloor.hq.app.AuditUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/** The first step of every change: refuses it, before anything is read or written, when the signed-in name is a
 *  viewer on the sheet (a viewer sees everything and changes nothing). */
public class GuardViewer {
    public void guard(@PathVariable("sheetId") String sheetId,
            @RequestHeader(name = "X-User", required = false) String user, RoleRepository roles)
            throws ViewerRefusedException {
        if (Role.VIEWER.equals(roles.roleOf(sheetId, AuditUser.decode(user)))) {
            throw new ViewerRefusedException("A viewer cannot change anything on the sheet.");
        }
    }
}
