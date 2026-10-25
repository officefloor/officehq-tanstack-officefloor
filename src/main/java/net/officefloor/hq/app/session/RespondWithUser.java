package net.officefloor.hq.app.session;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithUser {
    public void respond(@Val UserResponse user, ObjectResponse<UserResponse> response) {
        response.send(user);
    }
}
