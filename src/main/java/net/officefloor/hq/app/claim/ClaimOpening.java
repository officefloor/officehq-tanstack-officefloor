package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.voidreason.VoidReasonStore;
import net.officefloor.hq.app.watch.WatchStore;

/** A single open or a draft's amount edit, step by step, for a change that opens several claims at once (a paste). */
public final class ClaimOpening {
    private ClaimOpening() {
    }

    /** Edits a DRAFT to the amount as the claim form would, recording CLAIM_EDITED when the amount
     *  changes; refused as such an edit would be (locked by another user, a claim with lines, no longer a DRAFT). */
    public static void editAmount(Claim claim, ClaimInput input, ClaimStore store, ClaimLineStore lines,
            PersonStore people, RollupService rollups, Audit audit) throws InvalidSheetException {
        if (input.amount().compareTo(claim.getAmount()) == 0) {
            return;
        }
        new CheckClaimLock().check(input, claim, store);
        new CheckClaimLines().check(input, claim, lines);
        Rollup before = rollups.snapshot(claim.getSheetId());
        claim.setAmount(input.amount());
        claim.setEditedBy(input.user());
        if (!store.editAmount(claim)) {
            throw new InvalidSheetException("Only a DRAFT claim can be edited");
        }
        new AuditClaimEdited().audit(input, claim, before, people, audit, rollups);
    }

    /** Opens the claim by the user exactly as the open button would: refused as it would be, else
     *  numbered and recorded as CLAIM_OPENED, then how it moved the figures. */
    public static void open(Claim claim, String user, ClaimStore store, PersonStore people, PeriodStore periods,
            SettingStore settings, SheetStore sheets, RoleStore roles, RateStore rates, ClaimLogStore log,
            RollupService rollups, Audit audit, WatchStore watches, VoidReasonStore voidReasons)
            throws InvalidSheetException {
        StatusInput status = new StatusInput(ClaimStatus.OPEN, user, false, "");
        new CheckClaimPeriod().check(claim, periods);
        StatusMove[] move = new StatusMove[1];
        new CheckClerkLimit().check(status, claim, roles, settings, people, rates, sheets, store, log, m -> move[0] = m);
        Rollup before = rollups.snapshot(claim.getSheetId());
        StatusChange[] change = new StatusChange[1];
        new ApplyStatus().apply(status, move[0], claim, store, people, rates, settings, sheets, periods, voidReasons,
                c -> change[0] = c);
        new SaveClaim().save(status, claim, store, log);
        new AuditClaimStatus().audit(status, move[0], claim, change[0], before, people, audit, rollups, watches);
    }
}
