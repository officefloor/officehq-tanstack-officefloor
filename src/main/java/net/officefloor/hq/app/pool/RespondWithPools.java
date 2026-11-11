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
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's pot is the pot of each pool that is a member of none; a pool that is a member of another
 * has its cut in that pool as its pot. A read made as of a date (see {@link AsOf}) answers with the pools,
 * their members and the pot as they stood at the end of it. Each crew is a pool too, under its name, listed
 * among the pools by name: its lead and members at equal shares, the sheet's pot its pot. For every period
 * whose to-date is on or before the sheet's today, each member carries its cut as it stood at the end of that
 * period (the pools, their members, the pot and the holds as of that day, it standing in for the sheet's today).
 */
public class RespondWithPools {
    public void respond(@Val Sheet sheet, PoolRepository repository, CrewRepository crews, SettingsRepository settings,
            PeriodRepository periods, HttpServletRequest request, ObjectResponse<PoolsResponse> response) {
        LocalDate asOf = AsOf.of(request);
        List<PoolResponse> now = read(sheet.id(), sheet.today(), asOf, repository, crews, settings);
        List<Period> ended = (asOf == null ? periods.findAll(sheet.id()) : periods.findAllAsOf(sheet.id(), asOf))
                .stream().filter(p -> !p.to().isAfter(sheet.today())).toList();
        // pool name -> member key -> period name -> cut
        Map<String, Map<String, Map<String, String>>> history = new HashMap<>();
        for (Period period : ended) {
            for (PoolResponse pool : read(sheet.id(), period.to(), period.to(), repository, crews, settings)) {
                for (PoolMemberResponse m : pool.members()) {
                    history.computeIfAbsent(pool.name(), k -> new HashMap<>())
                            .computeIfAbsent(key(m), k -> new HashMap<>()).put(period.name(), m.cut());
                }
            }
        }
        response.send(new PoolsResponse(now.stream().map(pool -> pool.withMembers(pool.members().stream().map(m -> {
            Map<String, String> then = history.getOrDefault(pool.name(), Map.of()).getOrDefault(key(m), Map.of());
            return m.withPeriodCuts(ended.stream()
                    .map(period -> new PeriodCut(period.name(), then.getOrDefault(period.name(), ""))).toList());
        }).toList())).toList()));
    }

    /** A member's key within its pool: the person's id, or pool-<name> for a member pool. */
    private static String key(PoolMemberResponse m) {
        return m.memberPool().isEmpty() ? m.personId() : "pool-" + m.memberPool();
    }

    /** The pools, with {@code today} as the sheet's today, as they stood at the end of {@code asOf} (null: now). */
    private static List<PoolResponse> read(String sheetId, LocalDate today, LocalDate asOf, PoolRepository repository,
            CrewRepository crews, SettingsRepository settings) {
        List<PoolMember> members = new ArrayList<>(repository.members(sheetId, today, asOf));
        members.addAll(repository.crewMembers(sheetId, today, asOf));
        BigDecimal sheetPot = (asOf == null ? settings.find(sheetId) : settings.findAsOf(sheetId, asOf)).pot();
        List<Pool> pools = new ArrayList<>(repository.findAll(sheetId, asOf));
        Map<String, Long> crewIds = new HashMap<>();
        for (Crew c : crews.findAll(sheetId, asOf)) {
            crewIds.put(c.name(), c.id());
            pools.add(new Pool(c.id(), sheetId, c.name()));
        }
        pools.sort(Comparator.comparing(Pool::name));
        Map<String, List<PoolMember>> byPool = new HashMap<>();
        for (Pool p : pools) {
            byPool.put(p.name(), members.stream().filter(m -> m.pool().equals(p.name())).toList());
        }
        Map<String, BigDecimal> pots = new HashMap<>();
        return pools.stream()
                .map(p -> {
                    BigDecimal pot = pot(p.name(), members, byPool, sheetPot, pots);
                    Long crewId = crewIds.get(p.name());
                    return crewId == null ? PoolResponse.from(p, byPool.get(p.name()), pot)
                            : PoolResponse.ofCrew(p, crewId, byPool.get(p.name()), pot);
                })
                .toList();
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
