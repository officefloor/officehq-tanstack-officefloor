package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRole {
    public void respond(@Val RoleInput role, ObjectResponse<RoleResponse> response) {
        response.send(new RoleResponse(role.user(), role.role().shown()));
    }
}
