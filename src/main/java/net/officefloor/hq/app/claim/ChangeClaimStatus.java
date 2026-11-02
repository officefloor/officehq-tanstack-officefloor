package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Move a claim along its status: open, settle, approve, reject (with a reason) or void, each only from the
 *  statuses it allows; or defer an OPEN claim until a date (see {@link ClaimDeferral}); or raise its proofs count (see {@link ClaimProof}); or split an amount off an OPEN claim (see {@link ClaimSplit}); or re-date an OPEN or PENDING claim (see {@link ClaimRedate}). */
public class ChangeClaimStatus {
    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            @PathVariable("action") String action, @RequestBody ClaimActionRequest request, ServerHttpConnection connection, ClaimRepository repository,
            PeriodRepository periods, SettingsRepository settings, SheetRepository sheets, Audit audit, Rollup rollup, WatchRepository watches, PersonRepository people, Out<Claim> claim) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long cid;
        try {
            cid = Long.parseLong(claimId == null ? "" : claimId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such claim");
        }
        if ("defer".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimDeferral.apply(id, cid, request == null ? null : request.until(), by, repository, sheets,
                    periods, audit, rollup));
            return;
        }
        if ("split".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimSplit.apply(id, cid, request == null ? null : request.amount(), by, repository, periods,
                    settings, audit, rollup));
            return;
        }
        if ("redate".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimRedate.apply(id, cid, request == null ? null : request.date(), by, repository, periods,
                    settings, people, audit, rollup));
            return;
        }
        if ("proof".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimProof.apply(id, cid, by, repository, periods, audit));
            return;
        }
        claim.set(ClaimStatusChange.apply(id, cid, action,
                request == null ? null : request.reason(), by, repository, periods, settings, audit, rollup, watches));
    }
}
