package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Re-settles a SETTLED claim: freezes its due again at the figure it would have today as an open claim bound by no
 * statement (today's rates, grades, floor and late charge; n/a leaves it live). Only a steward re-settles, and a claim
 * whose due-by is in a closed period, or in any other status, cannot be. Returns the due it had before, for the audit.
 */
public class ApplyResettle {
    public Resettled apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository, PersonRepository people,
            RateRepository rates, PersonGradeRepository grades, PeriodRepository periods, RoleRepository roles,
            @RequestHeader(name = "X-User", required = false) String userHeader)
            throws ClaimStatusException, ClerkLimitException {
        String user = AuditUser.decode(userHeader);
        if (periods.isClosedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new ClaimStatusException("A claim in a closed period cannot be re-settled.");
        }
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new ClerkLimitException("Only a steward can re-settle a claim.");
        }
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be re-settled.");
        }
        BigDecimal old = claim.getDue();
        BigDecimal due = people.findById(claim.getPersonId())
                .map(p -> new ClaimFigures(rates.findBySheetIdOrderByGradeAscFromDateAscIdAsc(claim.getSheetId()),
                        grades.gradesOf(p), sheet).openDue(claim))
                .orElse(null);
        claim.setDue(due);
        repository.saveAndFlush(claim);
        return new Resettled(old, due);
    }
}
