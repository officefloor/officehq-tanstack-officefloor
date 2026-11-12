package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Moves each selected claim to the chosen person, in list order: its people in the order they were added, a
 *  person's claims newest first. Each goes through the very steps of a single move by the user (role, period,
 *  status, correction and retirement checks, then store and audit as CLAIM_REASSIGNED with its rollup lines). A
 *  claim that is not on the sheet, or that a single move would refuse, is refused and left as it is. */
public class ReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId, @Val BulkReassignInput input, ClaimStore store,
            PersonStore people, PeriodStore periods, SettingStore settings, RoleStore roles, RollupService rollups,
            Audit audit, Out<BulkReassignResult> result) {
        String sheet = sheetId.trim();
        Map<Long, Integer> personOrder = new HashMap<>();
        // A person's claims newest first, as listed: a claim moved to them is listed as their newest.
        Map<Long, Integer> listed = new HashMap<>();
        for (Claim claim : store.list(sheet)) {
            listed.put(claim.getId(), listed.size());
        }
        for (Person person : people.list(sheet)) {
            personOrder.put(person.getId(), personOrder.size());
        }
        int refused = 0;
        List<Claim> selected = new ArrayList<>();
        for (Long id : input.claimIds()) {
            Claim claim = id == null ? null : store.find(sheet, id);
            if (claim == null) {
                refused++;
            } else {
                selected.add(claim);
            }
        }
        selected.sort(Comparator.<Claim>comparingInt(c -> personOrder.getOrDefault(c.getPersonId(), Integer.MAX_VALUE))
                .thenComparing(c -> listed.getOrDefault(c.getId(), Integer.MAX_VALUE)));
        ReassignInput move = new ReassignInput(input.to(), input.user());
        int reassigned = 0;
        for (Claim claim : selected) {
            try {
                roles.refuseChange(sheet, input.user());
                new CheckClaimPeriod().check(claim, periods);
                Reassignment reassignment = CheckReassign.refuse(move, claim, roles, people, settings, true);
                Rollup before = rollups.snapshot(sheet);
                new ApplyReassign().apply(claim, reassignment, store);
                new AuditClaimReassigned().audit(move, claim, reassignment, before, audit, rollups);
                reassigned++;
            } catch (InvalidSheetException e) {
                refused++;
            }
        }
        result.set(new BulkReassignResult(reassigned, refused));
    }
}
