package net.officefloor.hq.app.role;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's roles; read as of a date, those in force at the end of it. */
public class RespondWithRoles {
    public void respond(@Val Sheet sheet, RoleRepository repository, HttpServletRequest request,
            ObjectResponse<RolesResponse> response) {
        LocalDate asOf = AsOf.of(request);
        response.send(new RolesResponse((asOf == null ? repository.findAll(sheet.id())
                : repository.findAsOf(sheet.id(), asOf)).stream().map(RoleResponse::from).toList()));
    }
}
