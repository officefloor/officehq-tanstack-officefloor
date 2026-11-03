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

/** Move the selected claims to one person together, in list order (people in the order they were added, each
 *  person's claims newest first), each by exactly the rules of moving one claim ({@link ClaimReassign}); a
 *  selected claim already that person's is re-listed as their newest. A claim that cannot be moved is left as
 *  it is and counted as refused. */
public class BulkReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @RequestBody BulkReassignRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, PersonRepository people, Audit audit, Rollup rollup,
            Out<BulkReassignResult> result) throws BadRequestException {
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
        if (request.personId() == null) {
            throw new BadRequestException("choose the person to move the claims to");
        }
        List<Claim> found = repository.inListOrder(id, selected);
        int refused = selected.size() - found.size();
        int reassigned = 0;
        for (Claim c : found) {
            try {
                ClaimReassign.apply(id, c.id(), request.personId(), true, by, repository, periods, settings, people, audit, rollup);
                reassigned++;
            } catch (BadRequestException e) {
                refused++;
            }
        }
        result.set(new BulkReassignResult(reassigned, refused));
    }
}
