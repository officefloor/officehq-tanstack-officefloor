package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Moves the claim to SETTLED, refusing it when its status does not allow that, and fixes its due at the
 *  rate its person's grade has in force on the claim's date: amount × rate / 100, rounded half up (none when no rate applies).
 *  The user who opened the claim (arriving as X-User) cannot settle it. */
public class SettleClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, PersonRepository people,
            RateRepository rates) throws InvalidClaimStatusException, SelfSettleException {
        if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(user == null ? "" : user.trim())) {
            throw new SelfSettleException("The user who opened a claim cannot settle it.");
        }
        ClaimTransition.to(claim, ClaimStatus.SETTLED);
        Person person = people.findById(claim.getPersonId()).orElseThrow();
        claim.setSettledDue(rates.findInForce(claim.getSheetId(), person.getGrade(), claim.getDate())
                .map((r) -> claim.getAmount().multiply(r.getRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(null));
    }
}
