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

/** Refuses, as the claim's own settle would, a settle the signed-in user cannot make, and changes nothing (one that
 *  would go PENDING is not refused). A bulk settle checks every claim first, so that it settles all or none. */
public class CheckClaimSettle {
    public void check(@Val Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            @Val Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            @RequestHeader(name = "X-User", required = false) String userHeader)
            throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        ApplyClaimStatus.run("settle", claim, repository, entityManager, people, rates, grades, periods, sheet, roles, log,
                voidReasons, userHeader, null, null, true);
    }
}
