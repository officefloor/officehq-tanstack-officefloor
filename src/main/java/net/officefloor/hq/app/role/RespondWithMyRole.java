package net.officefloor.hq.app.role;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The role the signed-in user acts in on the sheet; read as of a date, the role they had then. */
public class RespondWithMyRole {
    public void respond(@Val Sheet sheet, RoleRepository roles, HttpServletRequest request,
            ObjectResponse<RoleResponse> response) {
        String user = Audit.user(request);
        LocalDate asOf = AsOf.of(request);
        String role = asOf == null ? roles.roleOf(sheet.id(), user) : roles.roleOf(sheet.id(), user, asOf);
        response.send(new RoleResponse(user, role));
    }
}
