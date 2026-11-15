package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the selected claims to one person in list order (people in the order they were added, newest
 *  claim first within a person), each by exactly the rules of {@link ReassignClaim#reassign} and recorded
 *  as it would record it (its CLAIM_REASSIGNED line, then the rollup lines it moved). A claim that cannot
 *  be moved (the user's role, its status, a closed period, the person's retirement, or it is not on the
 *  sheet) is left as it is and counted as refused; every one is on an archived sheet. */
public class BulkReassignClaims {
    public void reassign(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody BulkReassignRequest request, ClaimRepository claims, PeriodRepository periods,
            PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, RoleRepository roles, Audit audit,
            Out<BulkReassignResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Set<Long> selected = new HashSet<>(request == null || request.claimIds() == null
                ? List.of() : request.claimIds());
        selected.remove(null);
        Person target = request == null || request.to() == null ? null : people.find(id, request.to());

        // The selected claims of the sheet, in list order.
        List<Claim> all = claims.list(id);
        List<Claim> ordered = new ArrayList<>();
        for (Person p : people.list(id)) {
            for (Claim c : all) {
                if (c.getPersonId() == p.getId() && selected.contains(c.getId())) ordered.add(c);
            }
        }

        boolean archived = sheets.isArchived(id);
        int moved = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Claim claim : ordered) {
            if (archived) continue;
            Reassignment reassignment;
            try {
                reassignment = ReassignClaim.reassign(user, claim, target, people, roles, periods, settings);
            } catch (ClaimStatusException | PeriodClosedException refused) {
                continue;
            }
            claims.reassign(claim);
            moved++;
            audit.record(RecordClaimStatus.line("CLAIM_REASSIGNED", user, claim, people)
                    + " from=" + reassignment.from() + " to=" + reassignment.to());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkReassignResult(moved, selected.size() - moved));
    }
}
