package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Opens the claim's editor for the user: locks the claim against every other user's edit button until the editor is
 * saved or cancelled, or the sheet's today changes. Only a DRAFT outside a closed period can be edited; a claim that
 * cannot, or that another user's editor holds, is refused and takes no lock.
 */
public class LockClaim {
    public void lock(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, Periods periods) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be edited; only a DRAFT can");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimException("A claim due inside a closed period may not be edited");
        }
        if (claim.lockedBy() != null && !claim.lockedBy().equals(user)) {
            throw new InvalidClaimException("The claim is being edited by " + claim.lockedBy());
        }
        // Conditional on it still being a draft no other editor holds, so a lock taken meanwhile is refused.
        if (!claims.lock(sheet, claim.id(), user)) {
            throw new InvalidClaimException("The claim is no longer a DRAFT, or another user is editing it");
        }
    }
}
