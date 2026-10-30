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
 * within a person), numbering each as it opens and auditing it as a single open would be. A selected
 * claim that cannot be opened (not DRAFT, dated inside a closed period, the user's role may not open, or its note is empty while its due is above the note threshold) is left as it is and counted
 * as refused; so is a selected id that is not a claim on the sheet.
 */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @RequestBody BulkOpenRequest request,
            Claims claims, People people, Periods periods, Settings settings, Sheets sheets, Rates rates, ClaimNumbering numbering, Rollup rollup, Audit audit, ClaimLog log,
            ObjectResponse<BulkOpenResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        List<Claim> all = claims.list(sheetId);
        // A role the sheet's open permission does not allow opens none of them.
        boolean allowed = settings.allows(sheetId, request.user(), "open");
        int opened = 0;
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !selected.contains(String.valueOf(claim.getId()))) {
                    continue;
                }
                if (!allowed || !claim.getStatus().canBecome(ClaimStatus.OPEN)
                        || periods.isClosed(sheetId, claim.getDate())) {
                    continue;
                }
                try {
                    ApplyClaimStatus.refuseNoteless(claim, sheets, people, rates, settings,
                            Rounding.of(claim, settings.policyDate(sheetId)));
                } catch (ClaimStatusRefusedException e) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                claim.setNumber(numbering.next(sheetId, claim.getDate()));
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
