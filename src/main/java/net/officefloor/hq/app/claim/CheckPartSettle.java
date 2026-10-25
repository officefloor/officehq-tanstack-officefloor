package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Before a part-settle splits anything: refuses, as the claim's own settle would, a settle the signed-in user cannot
 *  make of the original claim (its four-eyes rules and its proof rule are judged on it), and changes nothing. */
public class CheckPartSettle {
    public void check(@Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            @RequestHeader(name = "X-User", required = false) String userHeader)
            throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        ApplyClaimStatus.run("settle", claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log,
                voidReasons, userHeader, null, null, true);
    }
}
