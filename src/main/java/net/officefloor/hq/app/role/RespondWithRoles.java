package net.officefloor.hq.app.role;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRoles {
    public void respond(@Val Sheet sheet, RoleRepository repository, ObjectResponse<RolesResponse> response) {
        response.send(new RolesResponse(
                repository.findAll(sheet.id()).stream().map(RoleResponse::from).toList()));
    }
}
