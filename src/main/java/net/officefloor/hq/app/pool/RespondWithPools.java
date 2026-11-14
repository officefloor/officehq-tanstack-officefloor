package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's pools as the screen reads them, in the order they were added, with each member's cut of its pool's pot,
 * now and at the end of each period ended by the sheet's today.
 */
public class RespondWithPools {
    public void respond(@Val Pools pools, @Val PoolHistory history, ObjectResponse<List<PoolResponse>> response) {
        response.send(pools.list().stream().map(p -> PoolResponse.from(p, pools.potOf(p), history)).toList());
    }
}
