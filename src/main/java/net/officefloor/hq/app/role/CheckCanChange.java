package net.officefloor.hq.app.role;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Refuses any change by a viewer, before anything is read or written. A viewer sees every screen and
 * figure and can change nothing. Runs first on every change endpoint except the bulk ones, which go
 * through with every selected item refused.
 */
public class CheckCanChange {
    public void check(@PathVariable("sheetId") String sheetId, RoleRepository roles, HttpServletRequest request)
            throws ViewerException {
        String user = Audit.user(request);
        if (Role.VIEWER.equals(roles.roleOf(sheetId == null ? "" : sheetId.trim(), user))) {
            throw new ViewerException("A viewer may not change the sheet");
        }
    }
}
