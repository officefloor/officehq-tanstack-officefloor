package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim and fixes its due: its amount times its person's grade's rate in force on the
 * claim's date, divided by 100 and rounded half to even to two decimals. With no such rate the due is
 * not fixed.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates)
            throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.SETTLED);
        Person person = people.find(sheet.id(), claim.getPersonId());
        claim.setSettledDue(Rate.inForce(rates.findAll(sheet.id()), person.getGrade(), claim.getDate())
                .map((pct) -> claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_EVEN))
                .orElse(null));
    }
}
