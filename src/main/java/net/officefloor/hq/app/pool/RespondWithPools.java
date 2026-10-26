package net.officefloor.hq.app.pool;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The sheet's pools as the screen shows them, each member with its cut as it stood at the end of every period whose
 * to-date is on or before the sheet's today: the sheet read as of that to-date.
 */
public class RespondWithPools {

    private record Shown(List<PoolResponse> pools, List<Period> ended) {
    }

    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<PoolResponse>> response) {
        Shown shown = history.view(sheet.id(), asOf.on(), jdbc -> {
            LocalDate today = new SheetRepository(jdbc).today(sheet.id());
            List<Period> ended = new PeriodRepository(jdbc).list(sheet.id()).stream()
                    .filter(p -> today != null && !p.to().isAfter(today)).toList();
            return new Shown(new PoolRepository(jdbc).list(sheet.id()), ended);
        });
        // Pool name, then member key, then period name: the member's cut at the end of that period.
        Map<String, Map<String, Map<String, String>>> past = new HashMap<>();
        for (Period period : shown.ended()) {
            List<PoolResponse> then = history.view(sheet.id(), period.to(),
                    jdbc -> new PoolRepository(jdbc).list(sheet.id()));
            for (PoolResponse pool : then) {
                for (PoolResponse.Member m : pool.members()) {
                    if (m.cut() != null) {
                        past.computeIfAbsent(pool.name(), k -> new HashMap<>())
                                .computeIfAbsent(m.key(), k -> new HashMap<>()).put(period.name(), m.cut());
                    }
                }
            }
        }
        List<PoolResponse> pools = new ArrayList<>();
        for (PoolResponse pool : shown.pools()) {
            List<PoolResponse.Member> members = new ArrayList<>();
            for (PoolResponse.Member m : pool.members()) {
                Map<String, String> cuts = past.getOrDefault(pool.name(), Map.of()).getOrDefault(m.key(), Map.of());
                Map<String, String> periodCuts = new LinkedHashMap<>();
                shown.ended().forEach(p -> periodCuts.put(p.name(), cuts.get(p.name())));
                members.add(m.withPeriodCuts(periodCuts));
            }
            pools.add(new PoolResponse(pool.name(), members, pool.totalShare(), pool.balanced()));
        }
        response.send(pools);
    }
}
