package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
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
 * Moves each selected claim to the chosen person in list order — the people in the order they were added, each one's
 * claims newest first, taken before any moves so a moved claim is not met again. Each is moved exactly as moving it
 * alone would (the same refusals, the same audit and rollup lines); one that cannot be moved is left as it is and
 * counted as refused.
 */
public class ReassignClaims {
    public void reassign(@Val SheetResponse sheet, @Val BulkReassignIds selected, @Val ChangedBy by,
            ClaimRepository claims, PersonRepository people, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods, Rollup rollup, Audit audit,
            Out<BulkReassignResponse> result) {
        int moved = 0;
        if (roles.isViewer(sheet.id(), by.name()) || sheets.isArchived(sheet.id())) {
            // A viewer's press, or one on an archived sheet, goes through with nothing done: every selected claim is
            // refused.
            result.set(new BulkReassignResponse(0, selected.ids().size()));
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
        ApplyReassign apply = new ApplyReassign();
        RefuseClosedClaim closed = new RefuseClosedClaim();
        RecordClaimReassigned record = new RecordClaimReassigned();
        for (Claim listed : ordered) {
            try {
                // re-read: an earlier move relisted the sheet's claims
                Claim claim = claims.find(sheet.id(), listed.getId());
                if (claim == null) {
                    continue;
                }
                closed.check(claim, periods);
                RollupSnapshot before = rollup.snapshot(sheet.id());
                ReassignTo to = new ReassignTo(selected.personId());
                apply.move(to, claim, by, claims, people, roles, settings, sheets, periods, true);
                record.record(claim, to, by, audit);
                rollup.record(sheet.id(), before, by);
                moved++;
            } catch (BadRequestException refused) {
                // left as it is
            }
        }
        // A selected id not on this sheet's list cannot be moved either.
        result.set(new BulkReassignResponse(moved, selected.ids().size() - moved));
    }
}
