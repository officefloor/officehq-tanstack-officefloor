package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes an open period, fixing its due-total as it is now: the figures of the claims that count whose date falls
 *  in it, added up (skipping n/a). Closing a period already closed is refused. */
public class ClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods,
            ClaimRepository claims, PersonRepository people, RateRepository rates, Out<PeriodClose> closed)
            throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        Map<Long, String> grades = people.list(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, Person::grade));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.list(period.sheetId())) {
            if (!ClaimDue.counts(c) || c.date().isBefore(period.from()) || c.date().isAfter(period.to())) {
                continue;
            }
            String grade = grades.get(c.personId());
            BigDecimal figure = grade == null ? c.due() : ClaimDue.figure(c, grade, rates);
            if (figure != null) {
                total = total.add(figure);
            }
        }
        if (!periods.close(period, total)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        closed.set(new PeriodClose(request.by(), total));
    }
}
