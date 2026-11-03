package net.officefloor.hq.app.pool;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's pools, each member with its cut at the end of every period whose to-date is on or before the
 *  sheet's today: the pools as they stood at the end of the period's to-date (see {@link AsOfView}). */
public class LoadPools {

    /** The pools as shown, with the periods ended by the sheet's today as shown. */
    private record Shown(List<Pool> pools, List<Period> ended) {
    }

    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PoolRepository repository, PeriodRepository periods, AsOfView view, Out<Pools> pools)
            throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Shown shown = view.read(connection, id, () -> {
            String today = periods.today(id);
            List<Period> ended = today == null ? List.of()
                    : periods.list(id).stream().filter(p -> p.to().compareTo(today) <= 0).toList();
            return new Shown(repository.list(id), ended);
        });
        // Each period's cuts then, by pool name then member key.
        Map<String, Map<String, Map<String, String>>> then = new LinkedHashMap<>();
        for (Period period : shown.ended()) {
            Map<String, Map<String, String>> cuts = new HashMap<>();
            for (Pool pool : view.readAt(id, LocalDate.parse(period.to()), () -> repository.list(id))) {
                Map<String, String> of = new HashMap<>();
                for (PoolMember m : pool.members()) of.put(m.key(), m.cut());
                cuts.put(pool.name(), of);
            }
            then.put(period.name(), cuts);
        }
        List<Pool> result = new ArrayList<>();
        for (Pool pool : shown.pools()) {
            List<PoolMember> members = new ArrayList<>();
            for (PoolMember m : pool.members()) {
                Map<String, String> periodCuts = new LinkedHashMap<>();
                for (Map.Entry<String, Map<String, Map<String, String>>> e : then.entrySet()) {
                    periodCuts.put(e.getKey(), e.getValue().getOrDefault(pool.name(), Map.of()).get(m.key()));
                }
                members.add(m.withPeriodCuts(periodCuts));
            }
            result.add(new Pool(pool.name(), members, pool.totalShare(), pool.balanced(), pool.added()));
        }
        pools.set(new Pools(result));
    }
}
