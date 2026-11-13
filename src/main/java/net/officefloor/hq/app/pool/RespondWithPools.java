package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's pools as the screen reads them, in the order they were added, with each member's cut of the pot. */
public class RespondWithPools {
    public void respond(@Val Pools pools, ObjectResponse<List<PoolResponse>> response) {
        response.send(pools.list().stream().map(p -> PoolResponse.from(p, pools.pot())).toList());
    }
}
