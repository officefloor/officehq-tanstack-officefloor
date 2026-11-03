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
 *  statuses it allows; or defer an OPEN claim until a date (see {@link ClaimDeferral}); or raise its proofs count (see {@link ClaimProof}); or split an amount off an OPEN claim (see {@link ClaimSplit}); or re-date an OPEN or PENDING claim (see {@link ClaimRedate}); or lock or unlock a claim's editor (see {@link ClaimLock}); or remind an OPEN or PENDING claim (see {@link ClaimRemind}); or comment a claim (see {@link ClaimComment}); or delete a DRAFT claim or restore it the same day (see {@link ClaimDelete}); or add a line to a DRAFT claim (see {@link ClaimLine}); or waive an OPEN or PENDING claim's late charge (see {@link ClaimWaive}); or hold or release an OPEN or PENDING claim (see {@link ClaimHold}). */
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
        if ("lock".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimLock.lock(id, cid, by, repository, periods));
            return;
        }
        if ("unlock".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimLock.unlock(id, cid, by, repository));
            return;
        }
        if ("remind".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimRemind.apply(id, cid, by, repository, audit));
            return;
        }
        if ("comment".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimComment.apply(id, cid, request == null ? null : request.text(), by, repository, audit));
            return;
        }
        if ("delete".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimDelete.delete(id, cid, by, repository, periods, settings, audit));
            return;
        }
        if ("restore".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimDelete.restore(id, cid, by, repository, periods, settings, audit));
            return;
        }
        if ("line".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimLine.apply(id, cid, request == null ? null : request.label(), request == null ? null : request.qty(),
                    request == null ? null : request.unit(), by, repository, periods, settings, audit));
            return;
        }
        if ("waive".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimWaive.apply(id, cid, by, repository, settings, audit, rollup));
            return;
        }
        if ("hold".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimHold.toggle(id, cid, by, repository, settings, audit, rollup));
            return;
        }
        if ("proof".equals(action == null ? "" : action.trim())) {
            claim.set(ClaimProof.apply(id, cid, request == null ? null : request.kind(), by, repository, periods, audit));
            return;
        }
        claim.set(ClaimStatusChange.apply(id, cid, action,
                request == null ? null : request.reason(), by, repository, periods, settings, audit, rollup, watches));
    }
}
