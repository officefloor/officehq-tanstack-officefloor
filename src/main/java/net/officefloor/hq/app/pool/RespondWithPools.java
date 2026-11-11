package net.officefloor.hq.app.pool;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's pot is the pot of each pool that is a member of none; a pool that is a member of another
 * has its cut in that pool as its pot. A read made as of a date (see {@link AsOf}) answers with the pools,
 * their members and the pot as they stood at the end of it.
 */
public class RespondWithPools {
    public void respond(@Val Sheet sheet, PoolRepository repository, SettingsRepository settings,
            HttpServletRequest request, ObjectResponse<PoolsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<PoolMember> members = repository.members(sheet.id(), sheet.today(), asOf);
        BigDecimal sheetPot = (asOf == null ? settings.find(sheet.id()) : settings.findAsOf(sheet.id(), asOf)).pot();
        List<Pool> pools = repository.findAll(sheet.id(), asOf);
        Map<String, List<PoolMember>> byPool = new HashMap<>();
        for (Pool p : pools) {
            byPool.put(p.name(), members.stream().filter(m -> m.pool().equals(p.name())).toList());
        }
        Map<String, BigDecimal> pots = new HashMap<>();
        response.send(new PoolsResponse(pools.stream()
                .map(p -> PoolResponse.from(p, byPool.get(p.name()), pot(p.name(), members, byPool, sheetPot, pots)))
                .toList()));
    }

    /** The pool's pot: the sheet's when it is a member of no pool, else its cut in the pool above (null for none). */
    private static BigDecimal pot(String pool, List<PoolMember> members, Map<String, List<PoolMember>> byPool,
            BigDecimal sheetPot, Map<String, BigDecimal> pots) {
        if (pots.containsKey(pool)) return pots.get(pool);
        BigDecimal pot = sheetPot;
        for (PoolMember m : members) {
            if (!pool.equals(m.memberPool())) continue;
            List<PoolMember> above = byPool.get(m.pool());
            List<BigDecimal> cuts = PoolResponse.cutsOf(above, pot(m.pool(), members, byPool, sheetPot, pots));
            pot = cuts == null ? null : cuts.get(above.indexOf(m));
            break;
        }
        pots.put(pool, pot);
        return pot;
    }
}
