package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.RoundingMode;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow. Opening gives
 * the claim the sheet's next number and records who opened it; a voided claim keeps its number (never reused). The one who opened a
 * claim cannot settle it. Settling fixes the claim's due at its person's grade's
 * rate in force on the claim's date (or at the figure its person's last statement bound it to) (none when no rate applies then, so it stays live). A claim dated in a closed period cannot change status. Returns the claim's number for the
 * audit.
 */
public class ApplyClaimStatus {
    public Integer apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PeriodRepository periods,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws ClaimStatusException {
        String user = AuditUser.decode(userHeader);
        if (periods.isClosedOn(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusException("A claim in a closed period cannot change status.");
        }
        if ("open".equals(action) && claim.getStatus().after(action) != null) {
            // Take the number first: it locks the sheet, then re-read the claim so a concurrent open is seen.
            repository.takeNextNumber(claim.getSheetId());
            entityManager.refresh(claim);
            ClaimStatus next = check(claim, action);
            claim.setNumber(repository.lastNumber(claim.getSheetId()));
            claim.setStatus(next);
            claim.setOpenedBy(user);
        } else {
            ClaimStatus next = check(claim, action);
            if (next == ClaimStatus.SETTLED && user.equals(claim.getOpenedBy())) {
                throw new ClaimStatusException("The one who opened a claim cannot settle it.");
            }
            claim.setStatus(next);
            if (claim.getStatus() == ClaimStatus.SETTLED) {
                claim.setDue(claim.isStated() ? claim.getStatedDue() : people.findById(claim.getPersonId())
                        .flatMap(p -> rates.findInForce(claim.getSheetId(), p.getGrade(), claim.getDate()))
                        .map(Rate::getPct)
                        .map(pct -> claim.getAmount().multiply(pct).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN))
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
