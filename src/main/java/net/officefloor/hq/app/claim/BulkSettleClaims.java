package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
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

/** Settle the selected claims together, all or nothing: when any of them cannot be settled by the signed-in
 *  user (any refusal the single settle would give; going to PENDING is not one), none is settled, every one
 *  counts as refused and nothing is written. Otherwise each is settled in list order (people in list order
 *  were added, each person's claims newest first) by exactly the rules of settling one claim (a claim a clerk
 *  sends to PENDING counts as settled); they are the sheet's next batch, each keeps its number, and after the
 *  claims' own lines BATCH_SETTLED batch=<n> count=<claims> total=<the frozen dues of those SETTLED> is written. */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @RequestBody BulkSettleRequest request,
            ServerHttpConnection connection, ClaimRepository repository, PeriodRepository periods,
            SettingsRepository settings, Audit audit, Rollup rollup, WatchRepository watches, Out<BulkSettleResult> result) throws BadRequestException {
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
        boolean refusedAny = found.size() < selected.size();
        for (Claim c : found) {
            if (refusedAny) break;
            try {
                ClaimStatusChange.check(id, c.id(), "settle", by, repository, periods, settings);
            } catch (BadRequestException e) {
                refusedAny = true;
            }
        }
        if (refusedAny) {
            result.set(new BulkSettleResult(0, selected.size()));
            return;
        }
        List<Long> settled = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : found) {
            Claim changed = ClaimStatusChange.apply(id, c.id(), "settle", by, repository, periods, settings, audit, rollup, watches);
            settled.add(c.id());
            if ("SETTLED".equals(changed.status()) && changed.due() != null) {
                total = total.add(new BigDecimal(changed.due()));
            }
        }
        int batch = repository.batch(id, settled);
        audit.record("BATCH_SETTLED sheet=" + id + " by=" + by + " batch=" + batch + " count=" + settled.size()
                + " total=" + total.toPlainString());
        result.set(new BulkSettleResult(settled.size(), 0));
    }
}
