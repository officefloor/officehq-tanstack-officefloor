package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Holds or releases the claim: only an OPEN or PENDING claim is. While held and not bound by a statement it counts in
 *  no live total, as a claim dated within a hold of its person does (see ClaimDue#countsLive). A claim already as asked
 *  is left as it is. */
public class ApplyClaimHold {
    public void apply(@Val Claim claim, @Val ClaimHold hold, Out<Claim> changed, Out<ClaimHoldChange> change)
            throws InvalidClaimStatusException {
        if (!ClaimDue.isOpen(claim)) {
            throw new InvalidClaimStatusException("Only an OPEN or PENDING claim can be held or released");
        }
        change.set(new ClaimHoldChange(claim.held() != hold.held()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(),
                claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(),
                claim.splitFrom(), claim.ref(), claim.tags(), hold.held()));
    }
}
