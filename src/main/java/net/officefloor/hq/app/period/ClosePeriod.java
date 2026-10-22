package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Close the named period, fixing its due-total as it is now: the figures (as shown on their rows) of the claims
 *  that count whose date falls in it, skipping n/a. Refused when there is no such period, or it is already
 *  closed. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            PeriodRepository periods, ClaimRepository claims, PersonRepository people, RateRepository rates,
            Out<Period> closed) throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        Period period = periods.find(id, periodName);
        if (period == null) {
            throw new InvalidPeriodException("There is no period " + periodName.trim() + " on this sheet.");
        }
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        Map<Long, Person> byId = people.list(id).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        BigDecimal total = new BigDecimal("0.00");
        for (Claim c : claims.list(id)) {
            if (!c.status().counts() || !period.contains(c.date())) {
                continue;
            }
            Person person = byId.get(c.personId());
            BigDecimal figure = Dues.figure(c, person != null ? person.grade() : "", rates);
            if (figure != null) {
                total = total.add(figure);
            }
        }
        if (!periods.close(period, total)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        closed.set(new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total));
    }
}
