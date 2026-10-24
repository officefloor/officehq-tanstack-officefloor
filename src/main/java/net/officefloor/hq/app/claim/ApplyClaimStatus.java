package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow. Opening gives
 * the claim the sheet's next number; voiding clears it. Settling fixes the claim's due at its person's grade's
 * rate in force on the claim's date (none when no rate applies then, so it stays live). Returns the number the claim had for the audit.
 */
public class ApplyClaimStatus {
    public Integer apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates) throws ClaimStatusException {
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
            if (claim.getStatus() == ClaimStatus.SETTLED) {
                claim.setDue(people.findById(claim.getPersonId())
                        .flatMap(p -> rates.findInForce(claim.getSheetId(), p.getGrade(), claim.getDate()))
                        .map(Rate::getPct)
                        .map(pct -> claim.getAmount().multiply(pct).movePointLeft(2).setScale(2, RoundingMode.HALF_UP))
                        .orElse(null));
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
