package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Open the selected claims together, in list order (people in the order they were added, each
 *  person's claims newest first), each numbered as it is opened. A claim that cannot be opened (no
 *  such claim, not a DRAFT, dated in a closed period) is left as it is and counted as refused. */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @RequestBody BulkOpenRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches, Out<BulkOpenResult> result) throws BadRequestException {
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
        int opened = 0;
        for (Claim c : found) {
            try {
                ClaimStatusChange.apply(id, c.id(), "open", by, repository, periods, settings, audit, rollup, watches);
                opened++;
            } catch (BadRequestException e) {
                refused++;
            }
        }
        result.set(new BulkOpenResult(opened, refused));
    }
}
