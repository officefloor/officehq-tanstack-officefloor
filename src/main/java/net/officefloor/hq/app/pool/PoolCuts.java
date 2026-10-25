package net.officefloor.hq.app.pool;

import java.util.Map;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.Sheet;

/** The pool members' cuts a period fixes when it is closed or soft-closed. */
public final class PoolCuts {

    private PoolCuts() {
    }

    /** Each pool member's cut as the sheet stands now (see RespondWithPools), by the member of the pool it is: the
     *  pool's id with the person or the member pool; "" for one with no cut. */
    public static Map<String, String> live(Sheet current, AsOf asOf) {
        return RespondWithPools.cutsOf(RespondWithPools.poolsAsOf(current, null, asOf));
    }
}
