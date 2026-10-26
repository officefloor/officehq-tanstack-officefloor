package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-dates each selected claim to the chosen date in list order — the people in their list positions, each one's
 * claims newest first. Each is re-dated exactly as re-dating it alone would (the same refusals, the same audit and
 * rollup lines); one that cannot be re-dated is left as it is and counted as refused.
 */
public class RedateClaims {
    public void redate(@Val SheetResponse sheet, @Val BulkRedateIds selected, @Val ChangedBy by,
            ClaimRepository claims, PersonRepository people, RoleRepository roles, SheetRepository sheets,
            PeriodRepository periods, Rollup rollup, Audit audit, Out<BulkRedateResponse> result) {
        int redated = 0;
        if (roles.isViewer(sheet.id(), by.name()) || sheets.isArchived(sheet.id())) {
            // A viewer's press, or one on an archived sheet, goes through with nothing done: every selected claim is
            // refused.
            result.set(new BulkRedateResponse(0, selected.ids().size()));
            return;
        }
        List<Claim> ordered = new ArrayList<>();
        for (Person person : people.list(sheet.id())) {
            for (Claim claim : claims.listOf(sheet.id(), person.getId())) {
                if (selected.ids().contains(claim.getId())) {
                    ordered.add(claim);
                }
            }
        }
        ApplyRedate apply = new ApplyRedate();
        RefuseClosedClaim closed = new RefuseClosedClaim();
        RecordClaimRedated record = new RecordClaimRedated();
        for (Claim listed : ordered) {
            try {
                Claim claim = claims.find(sheet.id(), listed.getId());
                if (claim == null) {
                    continue;
                }
                closed.check(claim, periods);
                RollupSnapshot before = rollup.snapshot(sheet.id());
                RedateTo to = new RedateTo(selected.date());
                apply.apply(to, claim, by, claims, people, roles);
                record.record(claim, to, by, people, audit);
                rollup.record(sheet.id(), before, by);
                redated++;
            } catch (BadRequestException refused) {
                // left as it is
            }
        }
        // A selected id not on this sheet's list cannot be re-dated either.
        result.set(new BulkRedateResponse(redated, selected.ids().size() - redated));
    }
}
