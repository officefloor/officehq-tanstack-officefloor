package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Closes the period, fixing its due-total as it stands now: the figures of the counting claims dated in it,
 * added up (n/a skipped). Refused (400) when it is already closed.
 */
public class ClosePeriod {
    public void close(@Val Period period, ClaimRepository claims, PersonRepository people, RateRepository rates,
            Out<Period> closed) throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        Map<Long, Person> byId = people.findAll(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, Function.identity()));
        List<Rate> sheetRates = rates.findAll(period.sheetId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.findAll(period.sheetId())) {
            Person person = byId.get(claim.personId());
            if (person == null || !person.counts(claim) || !period.contains(claim.date())) {
                continue;
            }
            BigDecimal figure = claim.figure(Rate.inForce(sheetRates, person.grade(), claim.date()));
            if (figure != null) {
                total = total.add(figure);
            }
        }
        closed.set(period.close(total));
    }
}
