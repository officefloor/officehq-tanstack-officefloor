package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;

/** Settles a claim, fixing its due: its amount at its person's grade's rate now, rounded half up to
 *  two decimals. With no rate the due is not fixed and stays live. */
public class SettleClaim {
    public void move(@Val Claim claim, PersonRepository people, RateRepository rates)
            throws ClaimStatusException {
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.SETTLED).name());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.getGrade());
        claim.setSettledDue(rate == null ? null
                : new BigDecimal(claim.getAmount()).multiply(new BigDecimal(rate.pct()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString());
    }
}
