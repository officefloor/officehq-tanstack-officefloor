package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPools {
    public void respond(@Val Pools pools, ObjectResponse<Pools> response) {
        response.send(pools);
    }
}
