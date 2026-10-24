package net.officefloor.hq.app.claim;

import jakarta.persistence.EntityManager;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;

/** Opens a claim exactly as the open button does (see {@link ApplyClaimStatus}), for features that open claims
 *  they make, such as the paste box. Not a step: kept out of ApplyClaimStatus so that keeps one public method. */
public final class OpenClaim {

    private OpenClaim() {
    }

    /** Opens the claim, refusing as the open button refuses; returns its number. */
    public static String open(Claim claim, ClaimRepository repository,
            EntityManager entityManager, PersonRepository people, RateRepository rates, PersonGradeRepository grades, PeriodRepository periods,
            Sheet sheet, RoleRepository roles, ClaimLogRepository log, VoidReasonRepository voidReasons,
            String userHeader) throws ClaimStatusException, ClerkLimitException, ProofRequiredException {
        return ApplyClaimStatus.run("open", claim, repository, entityManager, people, rates, grades, periods, sheet, roles,
                log, voidReasons, userHeader, null, null, false);
    }
}
