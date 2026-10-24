package net.officefloor.hq.app.paste;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPastedClaims {
    public void respond(@Val PastedClaims added, ObjectResponse<PastedClaims> response) {
        response.send(added);
    }
}
