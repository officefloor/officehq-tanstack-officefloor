package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total as it is now: the figures of the counted claims dated in it, added
 *  up, skipping n/a. Refuses a period already closed. */
public class ClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, ClaimRepository claims, PersonRepository people,
            RateRepository rates) throws InvalidPeriodException {
        if (period.isClosed()) {
            throw new InvalidPeriodException("The period is already closed.");
        }
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        BigDecimal total = claims.findBySheetIdOrderByIdDesc(period.getSheetId()).stream()
                .filter(c -> ClaimFigures.counts(c) && byId.containsKey(c.getPersonId()) && period.contains(c.getDate()))
                .map(c -> new ClaimFigures(rates.findBySheetIdAndGrade(period.getSheetId(),
                        byId.get(c.getPersonId()).getGrade())).figure(c))
                .filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        period.close(total);
        repository.saveAndFlush(period);
    }
}
