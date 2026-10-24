package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow. Opening gives
 * the claim the sheet's next number and records who opened it; a voided claim keeps its number (never reused). The one who opened a
 * claim cannot settle it, and a clerk (any name not made a steward) settles a claim only when its due is at or below
 * the sheet's clerk limit (a due of n/a is within any limit; no limit when the sheet has none). Settling fixes the claim's due at its person's grade's
 * rate in force on the claim's date (or at the figure its person's last statement bound it to) (none when no rate applies then, so it stays live). A claim dated in a closed period cannot change status. Returns the claim's number for the
 * audit.
 */
public class ApplyClaimStatus {
    public Integer apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws ClaimStatusException, ClerkLimitException {
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
            if (next == ClaimStatus.SETTLED) {
                BigDecimal due = claim.isStated() ? claim.getStatedDue() : people.findById(claim.getPersonId())
                        .flatMap(p -> rates.findInForce(claim.getSheetId(), p.getGrade(), claim.getDate()))
                        .map(Rate::getPct)
                        .map(pct -> claim.getAmount().multiply(pct).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN))
                        .orElse(null);
                BigDecimal limit = sheet.getClerkLimit();
                if (limit != null && due != null && due.compareTo(limit) > 0
                        && !Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
                    throw new ClerkLimitException("A clerk cannot settle a claim whose due is above the clerk limit.");
                }
                claim.setDue(due);
            }
            claim.setStatus(next);
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
