package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to SETTLED, refusing it when its status does not allow that, and fixes its due at the
 *  rate its person's grade has now: amount × rate / 100, rounded half up (none when the grade has no rate). */
public class SettleClaim {
    public void apply(@Val Claim claim, PersonRepository people, RateRepository rates)
            throws InvalidClaimStatusException {
        ClaimTransition.to(claim, ClaimStatus.SETTLED);
        Person person = people.findById(claim.getPersonId()).orElseThrow();
        claim.setSettledDue(rates.findBySheetIdAndGrade(claim.getSheetId(), person.getGrade())
                .map((r) -> claim.getAmount().multiply(r.getRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(null));
    }
}
