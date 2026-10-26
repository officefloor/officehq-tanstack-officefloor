package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRole {
    public void respond(@Val Role role, ObjectResponse<RoleResponse> response) {
        response.send(RoleResponse.from(role));
    }
}
