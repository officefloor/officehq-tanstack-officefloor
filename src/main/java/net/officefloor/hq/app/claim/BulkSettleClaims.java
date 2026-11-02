package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Settle the selected claims together, in list order (people in the order they were added, each
 *  person's claims newest first), each by exactly the rules of settling one claim: a claim a clerk sends
 *  to PENDING counts as settled. A claim that cannot be settled is left as it is and counted as refused. */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @RequestBody BulkSettleRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, Out<BulkSettleResult> result) throws BadRequestException {
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
        List<Claim> found = new ArrayList<>();
        int refused = 0;
        for (Long cid : selected) {
            Claim c = repository.find(id, cid);
            if (c == null) refused++;
            else found.add(c);
        }
        found.sort(Comparator.comparingLong(Claim::personId).thenComparing(Claim::id, Comparator.reverseOrder()));
        int settled = 0;
        for (Claim c : found) {
            try {
                ClaimStatusChange.apply(id, c.id(), "settle", by, repository, periods, settings, audit, rollup);
                settled++;
            } catch (BadRequestException e) {
                refused++;
            }
        }
        result.set(new BulkSettleResult(settled, refused));
    }
}
