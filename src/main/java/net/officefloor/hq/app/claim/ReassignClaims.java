package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Moves each selected claim that can move to the chosen person, in list order (people in list
 *  order, each person's newest claim first). Each is moved as a single reassignment would be — only
 *  by a role allowed to, only a DRAFT, OPEN or PENDING claim, never one due in a closed period nor
 *  dated after the person's retirement — with its own audit line, then the figures it moved. One
 *  already the person's is moved too: re-listed as their newest and unbound from any statement. One
 *  that cannot move (or is not on the sheet) is left as it is and counted as refused. */
public class ReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @Val BulkReassign bulk, Claims claims, People people,
            ClosedPeriods closedPeriods, Settings settings, Rollup rollup, Audit audit, Out<BulkReassignResult> result) {
        RecordClaimReassigned record = new RecordClaimReassigned();
        Person to = people.find(sheetId, bulk.toPersonId());
        int reassigned = 0;
        if (to != null && settings.allows(sheetId, "reassign", bulk.user())) {
            List<Claim> all = claims.onSheet(sheetId);
            for (Person person : people.onSheet(sheetId)) {
                for (Claim listed : all) {
                    if (listed.getPersonId() != person.getId() || !bulk.claimIds().contains(listed.getId())) continue;
                    Claim claim = claims.find(sheetId, listed.getId());
                    if (claim.getStatus() != ClaimStatus.DRAFT && !claim.getStatus().isOpen()) continue;
                    ClaimReassignment change = new ClaimReassignment(to.getId(), bulk.user());
                    Snapshot before = rollup.snapshot(sheetId);
                    try {
                        ApplyClaimReassign.move(change, claim, to, people, closedPeriods);
                    } catch (PersonRetiredException | PeriodClosedException e) {
                        continue;
                    }
                    claims.reassign(claim);
                    record.record(change, claim, audit);
                    rollup.record(sheetId, before, bulk.user());
                    reassigned++;
                }
            }
        }
        result.set(new BulkReassignResult(reassigned, bulk.claimIds().size() - reassigned));
    }
}
