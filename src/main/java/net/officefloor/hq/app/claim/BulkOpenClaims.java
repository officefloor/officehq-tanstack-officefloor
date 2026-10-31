package net.officefloor.hq.app.claim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Opens the selected claims in list order (people in the order they were added, newest claim first
 * within a person), unnumbered until settled, and auditing it as a single open would be. A selected
 * claim that cannot be opened (not DRAFT, its due-by inside a closed period, the user's role may not open, its note is empty while its due is above the note threshold, its person is flagged and the user is not a steward, or its person already has as many OPEN or PENDING claims in its period as the period's cap) is left as it is and counted
 * as refused; so is a selected id that is not a claim on the sheet. A claim whose due is above the review threshold goes to
 * REVIEW instead and counts as opened.
 */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @RequestBody BulkOpenRequest request,
            Claims claims, People people, Periods periods, Settings settings, Sheets sheets, Rates rates, Rollup rollup, Audit audit, ClaimLog log,
            ObjectResponse<BulkOpenResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        List<Claim> all = claims.list(sheetId);
        // A role the sheet's open permission does not allow opens none of them.
        boolean allowed = settings.allows(sheetId, request.user(), "open");
        boolean steward = Settings.STEWARD.equals(settings.role(sheetId, request.user()));
        int opened = 0;
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !selected.contains(String.valueOf(claim.getId()))) {
                    continue;
                }
                if (!allowed || !claim.getStatus().canBecome(ClaimStatus.OPEN)
                        || periods.isClosed(sheetId, claim.getPeriodDate())) {
                    continue;
                }
                java.math.RoundingMode rounding = Rounding.of(claim, settings.policyDate(sheetId), on -> settings.roundingMode(sheetId, on));
                try {
                    ApplyClaimStatus.refuseFlagged(claim, sheets, people, steward);
                    ApplyClaimStatus.refuseNoteless(claim, sheets, people, rates, settings, rounding);
                    ApplyClaimStatus.refuseOverCap(claim, periods, all);
                } catch (ClaimStatusRefusedException e) {
                    continue;
                }
                if (ApplyClaimStatus.needsReview(claim, sheets, people, rates, settings, rounding)) {
                    // Above the review threshold it goes to REVIEW, unnumbered, and counts as opened.
                    claim.setOpenedBy(request.user());
                    claim.setStatus(ClaimStatus.REVIEW);
                    claims.save(claim);
                    log.record(claim, person, request.user());
                    audit.recordOnCommit(ClaimAudit.line("CLAIM_REVIEW", request.user(), person, claim, null));
                    opened++;
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                claim.setOpenedBy(request.user());
                claim.setStatus(ClaimStatus.OPEN);
                claims.save(claim);
                log.record(claim, person, request.user());
                audit.recordOnCommit(ClaimAudit.line("CLAIM_OPENED", request.user(), person, claim, claim.getNumber()));
                rollup.recordChanges(sheetId, request.user(), before);
                opened++;
            }
        }
        response.send(new BulkOpenResponse(opened, selected.size() - opened));
    }
}
