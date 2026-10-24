package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow. Opening gives
 * the claim the sheet's next number; voiding clears it. Returns the number the claim had for the audit.
 */
public class ApplyClaimStatus {
    public Integer apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager) throws ClaimStatusException {
        if ("open".equals(action) && claim.getStatus().after(action) != null) {
            // Take the number first: it locks the sheet, then re-read the claim so a concurrent open is seen.
            repository.takeNextNumber(claim.getSheetId());
            entityManager.refresh(claim);
            ClaimStatus next = check(claim, action);
            claim.setNumber(repository.lastNumber(claim.getSheetId()));
            claim.setStatus(next);
        } else {
            claim.setStatus(check(claim, action));
            if (claim.getStatus() == ClaimStatus.VOID) {
                Integer had = claim.getNumber();
                claim.setNumber(null);
                repository.saveAndFlush(claim);
                return had;
            }
        }
        repository.saveAndFlush(claim);
        return claim.getNumber();
    }

    private static ClaimStatus check(Claim claim, String action) throws ClaimStatusException {
        ClaimStatus next = claim.getStatus().after(action);
        if (next == null) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot " + action + ".");
        }
        return next;
    }
}
