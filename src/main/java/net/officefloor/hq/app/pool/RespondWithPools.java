package net.officefloor.hq.app.pool;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's pot is the pot of each pool that is a member of none; a pool that is a member of another
 * has its cut in that pool as its pot. A read made as of a date (see {@link AsOf}) answers with the pools,
 * their members and the pot as they stood at the end of it. Each crew is a pool too, under its name, listed
 * among the pools by name: its lead and members at equal shares, the sheet's pot its pot.
 */
public class RespondWithPools {
    public void respond(@Val Sheet sheet, PoolRepository repository, CrewRepository crews, SettingsRepository settings,
            HttpServletRequest request, ObjectResponse<PoolsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<PoolMember> members = new ArrayList<>(repository.members(sheet.id(), sheet.today(), asOf));
        members.addAll(repository.crewMembers(sheet.id(), sheet.today(), asOf));
        BigDecimal sheetPot = (asOf == null ? settings.find(sheet.id()) : settings.findAsOf(sheet.id(), asOf)).pot();
        List<Pool> pools = new ArrayList<>(repository.findAll(sheet.id(), asOf));
        Map<String, Long> crewIds = new HashMap<>();
        for (Crew c : crews.findAll(sheet.id(), asOf)) {
            crewIds.put(c.name(), c.id());
            pools.add(new Pool(c.id(), sheet.id(), c.name()));
        }
        pools.sort(Comparator.comparing(Pool::name));
        Map<String, List<PoolMember>> byPool = new HashMap<>();
        for (Pool p : pools) {
            byPool.put(p.name(), members.stream().filter(m -> m.pool().equals(p.name())).toList());
        }
        Map<String, BigDecimal> pots = new HashMap<>();
        response.send(new PoolsResponse(pools.stream()
                .map(p -> {
                    BigDecimal pot = pot(p.name(), members, byPool, sheetPot, pots);
                    Long crewId = crewIds.get(p.name());
                    return crewId == null ? PoolResponse.from(p, byPool.get(p.name()), pot)
                            : PoolResponse.ofCrew(p, crewId, byPool.get(p.name()), pot);
                })
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
