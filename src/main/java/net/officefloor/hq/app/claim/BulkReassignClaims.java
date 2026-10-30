package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Moves the selected claims to one person, in list order (people in the order they were added, newest
 * claim first within a person, as listed before any of them moved), each by the rules of moving one
 * claim — the roles the reassign permission allows, a DRAFT, OPEN or PENDING claim, no due-by inside a
 * closed period, and no claim dated after the person's retirement — and audited as a single move would
 * be. A selected claim that cannot move is left as it is and counted as refused; so is a selected id
 * that is not a claim on the sheet.
 */
public class BulkReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @RequestBody BulkReassignRequest request,
            Claims claims, People people, Periods periods, Settings settings, Rollup rollup, Audit audit,
            ObjectResponse<BulkReassignResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        Person to = null;
        try {
            to = request.toPersonId() == null ? null : people.find(sheetId, Long.parseLong(request.toPersonId()));
        } catch (NumberFormatException e) {
            // not an id this app assigned
        }
        List<Person> listed = people.list(sheetId);
        List<Claim> all = claims.list(sheetId);
        List<Claim> ordered = new ArrayList<>();
        for (Person person : listed) {
            for (Claim claim : all) {
                if (claim.getPersonId() == person.getId() && selected.contains(String.valueOf(claim.getId()))) {
                    ordered.add(claim);
                }
            }
        }
        int reassigned = 0;
        if (to != null && settings.allows(sheetId, request.user(), "reassign")) {
            for (Claim claim : ordered) {
                ClaimStatus status = claim.getStatus();
                if (status != ClaimStatus.DRAFT && status != ClaimStatus.OPEN && status != ClaimStatus.PENDING) {
                    continue;
                }
                if (periods.isClosed(sheetId, claim.getPeriodDate()) || !to.allowsClaimOn(claim.getDate())) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                Person from = people.find(sheetId, claim.getPersonId());
                claim.setPersonId(to.getId());
                claim.setStated(false);
                claim.setStatedDue(null);
                claims.reassign(claim);
                audit.recordOnCommit(ClaimAudit.line("CLAIM_REASSIGNED", request.user(), to, claim, claim.getNumber())
                        + " from=" + from.getName() + " to=" + to.getName());
                rollup.recordChanges(sheetId, request.user(), before);
                reassigned++;
            }
        }
        response.send(new BulkReassignResponse(reassigned, selected.size() - reassigned));
    }
}
