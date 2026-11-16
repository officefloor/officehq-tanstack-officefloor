package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Moves each selected claim to the chosen person, in list order (people in the order they were added, a person's
 * claims newest first), by the checks of the single move (ApplyReassign: who may, which statuses, corrections, closed
 * periods, retirement), stored as SaveReassign stores it and written as RecordClaimReassigned writes it
 * (CLAIM_REASSIGNED and the figures it moved). A claim already the chosen person's counts as moved, listed as its
 * newest. Each that the move refuses, or is not on the sheet, is left as it is and counted refused.
 */
public class ReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @Val BulkReassignRequest request,
            ClaimRepository claims, PersonRepository people, RoleRepository roles, SettingRepository settings,
            PeriodRepository periods, Rollup rollup, Audit audit, Out<BulkReassignResult> result) {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request.claimIds());
        List<Person> listed = people.list(sheet);
        Map<Long, Integer> personOrder = new HashMap<>();
        for (int i = 0; i < listed.size(); i++) {
            personOrder.put(listed.get(i).id(), i);
        }
        List<Claim> chosen = claims.list(sheet).stream().filter((c) -> selected.contains(c.id()))
                .sorted((a, b) -> Integer.compare(personOrder.getOrDefault(a.personId(), Integer.MAX_VALUE),
                        personOrder.getOrDefault(b.personId(), Integer.MAX_VALUE)))
                .toList();
        ReassignRequest move = new ReassignRequest(request.personId(), request.by());
        SaveReassign save = new SaveReassign();
        RecordClaimReassigned record = new RecordClaimReassigned();
        int reassigned = 0;
        for (Claim listedClaim : chosen) {
            Claim claim = claims.find(sheet, listedClaim.id());
            if (claim == null) {
                continue;
            }
            RollupBefore before = rollup.take(sheet);
            Claim[] stored = new Claim[1];
            ClaimReassignment reassignment;
            try {
                reassignment = ApplyReassign.check(claim, move, people, roles, settings, periods, true);
                save.save(claim, reassignment, claims, (c) -> stored[0] = c);
            } catch (InvalidClaimStatusException refused) {
                continue;
            }
            reassigned++;
            record.record(stored[0], reassignment, before, rollup, audit);
        }
        result.set(new BulkReassignResult(reassigned, selected.size() - reassigned));
    }
}
