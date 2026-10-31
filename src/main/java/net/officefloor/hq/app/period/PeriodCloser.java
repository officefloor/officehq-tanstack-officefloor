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
import org.springframework.stereotype.Service;

/** Closes a period, fixing its due-total as the screen shows it now: the figures (as shown now) of
 *  everyone's claims dated in it that were counted (OPEN, PENDING or SETTLED) by the status each had
 *  at the end of the period's last day, as its status log records it — not deferred ones, nor
 *  ones open then and dated within their person's hold, unless stated — n/a ones skipped. Each
 *  person's due-total for it is fixed too, and the period's is their sum. Used by a close by hand
 *  and by the auto close of a today change. */
@Service
public class PeriodCloser {

    private final Periods periods;
    private final Claims claims;
    private final People people;
    private final Figures figures;
    private final ClaimLog log;

    public PeriodCloser(Periods periods, Claims claims, People people, Figures figures, ClaimLog log) {
        this.periods = periods;
        this.claims = claims;
        this.people = people;
        this.figures = figures;
        this.log = log;
    }

    /** Close the open period; the period as it is once closed. */
    public Period close(Period period) {
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
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total);
    }
}
