package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Figures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.pool.Pools;
import org.springframework.stereotype.Service;

/** Closes a period, fixing its due-total as the screen shows it now: the figures (as shown now) of
 *  everyone's claims due by in it (by the due-by fixed when each was added) that were counted (OPEN, PENDING or SETTLED) by the status each had
 *  at the end of the period's last day, as its status log records it — not deferred ones, nor
 *  ones open then and dated within their person's hold, unless stated — n/a ones skipped. Each
 *  person's due-total for it is fixed too, and the period's is their sum; and so is each person's
 *  carried figure for it: the figures of their counted claims belonging, by due-by, to a period ending
 *  before this one starts that were still OPEN or PENDING at the end of its last day, and their opening
 *  balance for it. Every pool member's cut for it is fixed as it stands now (see {@link Pools#fixCuts}).
 *  Used by a close by hand
 *  and by the auto close of a today change. */
@Service
public class PeriodCloser {

    private final Periods periods;
    private final Claims claims;
    private final People people;
    private final Figures figures;
    private final ClaimLog log;
    private final Pools pools;

    public PeriodCloser(Periods periods, Claims claims, People people, Figures figures, ClaimLog log, Pools pools) {
        this.pools = pools;
        this.periods = periods;
        this.claims = claims;
        this.people = people;
        this.figures = figures;
        this.log = log;
    }

    /** Close the open or soft-closed period; the period as it is once closed. */
    public Period close(Period period) {
        return close(period, false);
    }

    /** Close the period, or soft-close it when `soft`; the period as it is then. */
    public Period close(Period period, boolean soft) {
        Map<Long, Person> personOf = people.onSheet(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Long id : personOf.keySet()) personTotals.put(id, BigDecimal.ZERO.setScale(2));
        Map<Long, ClaimStatus> atEnd = log.statusesAt(period.sheetId(), period.to());
        for (Claim claim : claims.onSheet(period.sheetId())) {
            if (!period.contains(claim.getDueBy())) continue;
            Person person = personOf.get(claim.getPersonId());
            if (person == null || !person.counts(claim,
                    atEnd.containsKey(claim.getId()) ? atEnd.get(claim.getId()) : claim.getStatus())) continue;
            BigDecimal figure = figures.due(claim, person.gradeOn(claim.getDate()));
            if (figure != null) personTotals.merge(person.getId(), figure, BigDecimal::add);
        }
        Map<Long, BigDecimal> personCarried = carried(period, personOf, atEnd);
        BigDecimal total = personTotals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        periods.close(period, total, personTotals, personCarried, soft);
        pools.fixCuts(period.sheetId(), period.name());
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total, soft, period.cap(), period.locked());
    }

    /** Each person's carried figure for the period as the screen shows it now, by person id. */
    private Map<Long, BigDecimal> carried(Period period, Map<Long, Person> personOf, Map<Long, ClaimStatus> atEnd) {
        Map<Long, BigDecimal> personCarried = new LinkedHashMap<>();
        for (Long id : personOf.keySet()) personCarried.put(id, BigDecimal.ZERO.setScale(2));
        List<Period> all = periods.onSheet(period.sheetId());
        for (Claim claim : claims.onSheet(period.sheetId())) {
            Period home = all.stream().filter(p -> p.contains(claim.getDueBy())).findFirst().orElse(null);
            if (home == null || !home.to().isBefore(period.from())) continue;
            Person person = personOf.get(claim.getPersonId());
            ClaimStatus status = atEnd.containsKey(claim.getId()) ? atEnd.get(claim.getId()) : claim.getStatus();
            if (person == null || status == null || !status.isOpen() || !person.counts(claim, status)) continue;
            BigDecimal figure = figures.due(claim, person.gradeOn(claim.getDate()));
            if (figure != null) personCarried.merge(person.getId(), figure, BigDecimal::add);
        }
        periods.openings(period).forEach((id, amount) -> {
            if (personCarried.containsKey(id)) personCarried.merge(id, amount, BigDecimal::add);
        });
        return personCarried;
    }
}
