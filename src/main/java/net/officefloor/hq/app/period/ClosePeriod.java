package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total as the screen shows it now: the figures of everyone's
 *  counted (OPEN or SETTLED) claims dated in it, n/a ones skipped. A closed period cannot be
 *  closed again. Publishes the closed period. */
public class ClosePeriod {
    public void close(@Val Period period, Periods periods, Claims claims, People people, Figures figures,
            Out<ClosedPeriod> closed) throws PeriodClosedException {
        if (period.closed()) throw new PeriodClosedException("Period " + period.name() + " is already closed");
        Map<Long, String> gradeOf = people.onSheet(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Person::getGrade));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.onSheet(period.sheetId())) {
            if (!claim.getStatus().counts() || !period.contains(claim.getDate())) continue;
            String grade = gradeOf.get(claim.getPersonId());
            if (grade == null) continue;
            BigDecimal figure = figures.figure(claim, grade);
            if (figure != null) total = total.add(figure);
        }
        periods.close(period, total);
        closed.set(new ClosedPeriod(new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total)));
    }
}
