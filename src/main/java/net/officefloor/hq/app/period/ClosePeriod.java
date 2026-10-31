package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total as the screen shows it now: the figures (as shown now) of
 *  everyone's claims dated in it that were counted (OPEN, PENDING or SETTLED) by the status each had
 *  at the end of the period's last day, as its status log records it — not deferred ones, nor
 *  ones open then and dated within their person's hold, unless stated — n/a ones skipped. A closed period cannot be
 *  closed again. Each person's due-total for it is fixed too, and the
 *  period's is their sum. Publishes the closed period. */
public class ClosePeriod {
    public void close(@Val Period period, Periods periods, Claims claims, People people, Figures figures, ClaimLog log,
            Out<ClosedPeriod> closed) throws PeriodClosedException {
        if (period.closed()) throw new PeriodClosedException("Period " + period.name() + " is already closed");
        Map<Long, Person> personOf = people.onSheet(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Long id : personOf.keySet()) personTotals.put(id, BigDecimal.ZERO.setScale(2));
        Map<Long, ClaimStatus> atEnd = log.statusesAt(period.sheetId(), period.to());
        for (Claim claim : claims.onSheet(period.sheetId())) {
            if (!period.contains(claim.getDate())) continue;
            Person person = personOf.get(claim.getPersonId());
            if (person == null || !person.counts(claim,
                    atEnd.containsKey(claim.getId()) ? atEnd.get(claim.getId()) : claim.getStatus())) continue;
            BigDecimal figure = figures.due(claim, person.gradeOn(claim.getDate()));
            if (figure != null) personTotals.merge(person.getId(), figure, BigDecimal::add);
        }
        BigDecimal total = personTotals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        periods.close(period, total, personTotals);
        closed.set(new ClosedPeriod(new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total)));
    }
}
