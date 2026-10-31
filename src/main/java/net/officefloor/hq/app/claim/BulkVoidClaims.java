package net.officefloor.hq.app.claim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Voids the selected claims in list order (people in the order they were added, newest claim first within a
 * person), each by exactly the rules of voiding one claim and audited as a single void would be (its CLAIM_VOIDED
 * line, then any rollup lines it moved). While the sheet has void reasons, the chosen reason must be one of them,
 * else every selected claim is refused. A selected claim that cannot be voided is left as it is and counted as
 * refused; so is a selected id that is not a claim on the sheet.
 */
public class BulkVoidClaims {
    public void voidClaims(@PathVariable("sheetId") String sheetId, @RequestBody BulkVoidRequest request,
            Claims claims, Sheets sheets, People people, Rates rates, Periods periods, Settings settings,
            ClaimNumbering numbering, Rollup rollup, Audit audit, ClaimLog log, Watches watches, VoidReasons voidReasons,
            ObjectResponse<BulkVoidResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        try {
            settings.refuseViewer(sheetId, request.user());
        } catch (ViewerRefusedException e) {
            response.send(new BulkVoidResponse(0, selected.size()));
            return;
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (voidReasons.list(sheetId).isEmpty()) {
            reason = null;
        } else if (reason.isEmpty() || !voidReasons.contains(sheetId, reason)) {
            response.send(new BulkVoidResponse(0, selected.size()));
            return;
        }
        ValidClaimStatus voiding = new ValidClaimStatus(ClaimStatus.VOID, request.user(),
                ValidClaimStatus.Decision.NONE, reason);
        List<Claim> all = claims.list(sheetId);
        int voided = 0;
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !selected.contains(String.valueOf(claim.getId()))) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                ClaimNumber number;
                try {
                    number = ApplyClaimStatus.change(voiding, claim, sheets, people, rates, periods, settings, numbering, log);
                } catch (ClaimStatusRefusedException refused) {
                    continue;
                }
                claims.save(claim);
                AuditClaimStatus.record(voiding, claim, number, people, audit, before, rollup, log, watches);
                voided++;
            }
        }
        response.send(new BulkVoidResponse(voided, selected.size() - voided));
    }
}
