package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Moves the claim to the status the action leads to; refuses a change its status does not allow. Opening gives
 * the claim the sheet's next number (or, while the sheet's period numbering is on, the next in its period: <period>-<n>,
 * X-<n> for a claim in no period) and records who opened it; a voided claim keeps its number (never reused). The one who opened a
 * claim cannot settle it, and a clerk (any name not made a steward) settles a claim only when its due is at or below
 * the sheet's clerk limit (a due of n/a is within any limit; no limit when the sheet has none). Settling fixes the claim's due at the rate of the grade its
 * person had on the claim's date, in force on the claim's date, at least the sheet's floor (or at the figure its person's last statement bound it to), plus its late charge on the sheet's today (none when no rate applies then, so it stays live). A claim dated in a closed period cannot change status. Returns the claim's number for the
 * audit.
 */
public class ApplyClaimStatus {
    public String apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws ClaimStatusException, ClerkLimitException {
        String user = AuditUser.decode(userHeader);
        if (periods.isClosedOn(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusException("A claim in a closed period cannot change status.");
        }
        if ("open".equals(action) && claim.getStatus().after(action) != null) {
            // Lock the sheet first (taking a number locks it), then re-read the claim so a concurrent open is seen.
            boolean perPeriod = sheet.periodNumberingOn();
            if (perPeriod) {
                repository.lockSheet(claim.getSheetId());
            } else {
                repository.takeNextNumber(claim.getSheetId());
            }
            entityManager.refresh(claim);
            ClaimStatus next = check(claim, action);
            if (perPeriod) {
                String prefix = periods.findBySheetIdOrderByFromDateAscIdAsc(claim.getSheetId()).stream()
                        .filter(p -> p.contains(claim.getDate())).map(Period::getName).findFirst().orElse(null);
                String seq = prefix == null ? "X" : "P:" + prefix;
                claim.setNumber((prefix == null ? "X" : prefix) + "-" + repository.takeNextInSequence(claim.getSheetId(), seq));
            } else {
                claim.setNumber(String.valueOf(repository.lastNumber(claim.getSheetId())));
            }
            claim.setStatus(next);
            claim.setOpenedBy(user);
        } else {
            ClaimStatus next = check(claim, action);
            if (next == ClaimStatus.SETTLED && user.equals(claim.getOpenedBy())) {
                throw new ClaimStatusException("The one who opened a claim cannot settle it.");
            }
            if (next == ClaimStatus.SETTLED) {
                BigDecimal due = people.findById(claim.getPersonId())
                        .map(p -> new ClaimFigures(rates.findBySheetIdOrderByGradeAscFromDateAscIdAsc(claim.getSheetId()),
                                grades.gradesOf(p), sheet).due(claim))
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
