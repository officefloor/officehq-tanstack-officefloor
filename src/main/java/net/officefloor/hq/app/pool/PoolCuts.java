package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.HashMap;
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

    /** Each person's cut total as the sheet stands now: their cuts across every pool they are a member of, added up
     *  (a member with no cut adds nothing), by person id; a person in no pool is absent. */
    public static Map<Long, BigDecimal> totalsByPerson(Sheet current, AsOf asOf) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (PoolResponse pool : RespondWithPools.poolsAsOf(current, null, asOf)) {
            pool.members().stream().filter(m -> m.personId() != null && !m.cut().isEmpty())
                    .forEach(m -> totals.merge(m.personId(), new BigDecimal(m.cut()), BigDecimal::add));
        }
        return totals;
    }
}
