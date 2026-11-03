package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Re-date the selected claims together to one date, in list order (people in list order, each person's claims
 *  newest first), each by exactly the rules of re-dating one claim ({@link ClaimRedate}). A claim that cannot be
 *  re-dated is left as it is and counted as refused. */
public class BulkRedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId, @RequestBody BulkRedateRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, PersonRepository people, Audit audit, Rollup rollup,
            Out<BulkRedateResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.claimIds() != null) {
            for (Long cid : request.claimIds()) {
                if (cid != null) selected.add(cid);
            }
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("select at least one claim");
        }
        List<Claim> found = repository.inListOrder(id, selected);
        int refused = selected.size() - found.size();
        int redated = 0;
        for (Claim c : found) {
            try {
                ClaimRedate.apply(id, c.id(), request.date(), by, repository, periods, settings, people, audit, rollup);
                redated++;
            } catch (BadRequestException e) {
                refused++;
            }
        }
        result.set(new BulkRedateResult(redated, refused));
    }
}
