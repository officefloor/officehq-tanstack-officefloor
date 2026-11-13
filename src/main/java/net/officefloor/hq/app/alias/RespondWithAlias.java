package net.officefloor.hq.app.alias;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithAlias {
    public void respond(@Val Alias alias, ObjectResponse<AliasResponse> response) {
        response.send(AliasResponse.from(alias));
    }
}
