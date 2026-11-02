package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Claims claims, ObjectResponse<Claims> response) {
        response.send(claims);
    }
}
