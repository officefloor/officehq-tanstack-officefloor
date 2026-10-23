package net.officefloor.hq.app.pool;

import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools in the order they were added, with their members, total share and whether balanced; a member
 *  retired by the sheet's today (or the as-of date, when one is given) cuts 0.00, their share still counted toward the total
 *  and spread over the members with a cut (no cuts at all when every member is retired); each member of a balanced pool
 *  shows their cut of the pool's pot (when there is one). A pool that is a member of none has the sheet's pot (as it stood
 *  then, given an as-of date); a pool
 *  that is a member of another has its cut there as its pot (none when the pool above has no cuts). A person with a
 *  hold in force on that date cuts 0.00, their share spread over the members with a cut. A member whose membership
 *  dates do not include that date is left out: no share in the total and no cut. After the pools come the crews, each
 *  a pool under its name with its lead and then its members (in the order added) at equal shares (100.00 split as
 *  patrons' shares are, the remainder on the lead), cutting the sheet's pot like any pool. Given a period that is
 *  closed or soft-closed, each member shows instead the cut fixed for them when it was closed ('' when they had none
 *  then). */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf,
            @RequestParam(name = "preview", required = false) String preview,
            @RequestParam(name = "period", required = false) String periodName, PoolRepository repository,
            CrewRepository crews, PersonRepository people, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods, ObjectResponse<List<PoolResponse>> response) {
        String id = sheetId.trim();
        List<PoolResponse> all = ShownPools.list(id, AsOf.parse(asOf), AsOf.parse(preview), repository, crews, people, sheets, settings);
        Period period = periodName != null ? periods.find(id, periodName) : null;
        if (period != null && (period.closed() || period.soft())) {
            Map<String, String> fixed = repository.fixedCuts(id, period.name());
            all = all.stream().map(p -> p.withCuts(fixed)).toList();
        }
        response.send(all);
    }
}
