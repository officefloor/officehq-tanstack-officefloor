package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPoolAdded {
    public void respond(@Val PoolInput input, ObjectResponse<AddedResponse> response) {
        response.send(new AddedResponse(input.name()));
    }
}
