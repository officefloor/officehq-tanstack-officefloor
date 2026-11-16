package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the new status. Opening gives the claim its number (see ClaimNumbering). A change another user made first (the
 * stored status is no longer the one this change started from) is refused, and the transaction gives back the number.
 * A stored change adds its line to the claim's status log (see ClaimLog).
 */
public class SaveClaimStatus {
    public void save(@Val Claim claim, @Val ClaimStatusChange change, ClaimRepository claims, PeriodRepository periods,
            SettingRepository settings, SheetRepository sheets, ClaimLog log, Out<Claim> saved,
            Out<ClaimNumber> number) throws InvalidClaimStatusException {
        Claim toSave = claim;
        if (change.action() == ClaimAction.OPEN) {
            String next = ClaimNumbering.next(claim, claims, periods, settings, sheets);
            toSave = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), claim.status(), next, claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom());
            number.set(new ClaimNumber(next));
        }
        ClaimStatus from = switch (change.action()) {
            case OPEN -> ClaimStatus.DRAFT;
            case SETTLE -> ClaimStatus.OPEN;
            case APPROVE, REJECT -> ClaimStatus.PENDING;
            case RESETTLE -> ClaimStatus.SETTLED;
            case VOID -> null;
        };
        boolean stored = from != null ? claims.updateStatus(toSave, from)
                : claims.updateStatus(toSave, ClaimStatus.DRAFT) || claims.updateStatus(toSave, ClaimStatus.OPEN)
                        || claims.updateStatus(toSave, ClaimStatus.PENDING);
        if (!stored) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        log.write(toSave, change.by());
        saved.set(toSave);
    }
}
