package net.officefloor.hq.app.pool;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools, each member with their cut at the end of each period whose to-date is on or
 *  before the sheet's today: the pools worked out on the sheet as it stood at the end of that to-date
 *  (see {@link AsOf}), that day as the sheet's today. */
public class RespondWithPools {

    private record Now(List<PoolResponse> pools, Map<String, LocalDate> ended) {
    }

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Pools pools, AsOf asOfSheet,
            ObjectResponse<List<PoolResponse>> response) throws InvalidSheetRequestException {
        Now now = asOfSheet.view(sheetId, asOf, () -> new Now(pools.onSheet(sheetId), pools.periodsEnded(sheetId)));
        // Each ended period's cuts, by pool then member.
        Map<String, Map<String, Map<String, String>>> then = new LinkedHashMap<>();
        now.ended().forEach((period, to) -> {
            Map<String, Map<String, String>> byPool = new HashMap<>();
            for (PoolResponse p : asOfSheet.at(sheetId, to, () -> pools.onSheet(sheetId))) {
                Map<String, String> byMember = new HashMap<>();
                for (PoolMemberResponse m : p.members()) byMember.put(m.key(), m.cut());
                byPool.put(p.name(), byMember);
            }
            then.put(period, byPool);
        });
        List<PoolResponse> shown = new ArrayList<>(now.pools().size());
        for (PoolResponse p : now.pools()) {
            List<PoolMemberResponse> members = new ArrayList<>(p.members().size());
            for (PoolMemberResponse m : p.members()) {
                Map<String, String> cuts = new LinkedHashMap<>();
                then.forEach((period, byPool) -> cuts.put(period,
                        byPool.getOrDefault(p.name(), Map.of()).get(m.key())));
                members.add(m.withCuts(cuts));
            }
            shown.add(new PoolResponse(p.name(), members, p.totalShare(), p.balanced(), p.crew()));
        }
        response.send(shown);
    }
}
