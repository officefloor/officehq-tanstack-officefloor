package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the move: the claim keeps its date and, when it has a number, receives the next one in the sequence it would
 * be numbered in now (see ClaimNumbering; its old number is never used again — a draft gets none), loses any figure a statement bound it to (its due is live
 * again until its new person's next statement) and is listed as its new person's newest claim. A change another user
 * made first (the claim moved or its status changed) is refused.
 */
public class SaveReassign {
    public void save(@Val Claim claim, @Val ClaimReassignment reassignment, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, SheetRepository sheets, Out<Claim> saved)
            throws InvalidClaimStatusException {
        if (!claims.reassign(claim, reassignment.from().id(), reassignment.to().id())) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        Claim moved = claims.find(claim.sheetId(), claim.id());
        if (moved.number() != null) {
            claims.renumber(moved, ClaimNumbering.next(moved, claims, periods, settings, sheets));
            moved = claims.find(claim.sheetId(), claim.id());
        }
        saved.set(moved);
    }
}
