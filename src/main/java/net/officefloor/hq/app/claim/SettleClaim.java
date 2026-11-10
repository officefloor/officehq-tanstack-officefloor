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
 * Settles the claim and fixes its due: its amount times its person's grade's rate now, divided by
 * 100 and rounded half up to two decimals. With no rate for the grade the due is not fixed.
 */
public class SettleClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, PersonRepository people, RateRepository rates)
            throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.SETTLED);
        Person person = people.find(sheet.id(), claim.getPersonId());
        claim.setSettledDue(rates.findAll(sheet.id()).stream()
                .filter((r) -> r.grade().equals(person.getGrade())).findFirst()
                .map(Rate::pct)
                .map((pct) -> claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP))
                .orElse(null));
    }
}
