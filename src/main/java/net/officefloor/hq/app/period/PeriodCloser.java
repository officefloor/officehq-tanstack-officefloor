package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Hold;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import org.springframework.stereotype.Service;

/** Close an open period: fix its due-total as shown now (the dues now, late charges included, of the claims whose
 *  due-by falls in it that count by the status they had at the end of its last day, a person's open (OPEN or PENDING) claims dated
 *  within one of their holds left out unless bound by a statement, skipping those with none), so later changes to
 *  rates do not alter it; and each person's due-total for it likewise, for every person on the sheet (0.00 for one
 *  with nothing in it), and each person's carried figure for it (their claims that count whose due-by falls in a
 *  period ending before it starts and that were still OPEN or PENDING at the end of its last day, plus their opening
 *  balance for it, 0.00 for one with none). Writes PERIOD_CLOSED, ending auto=yes when closed by the sheet's grace days. */
@Service
public class PeriodCloser {

    private final PeriodRepository periods;
    private final ClaimRepository claims;
    private final PersonRepository people;
    private final SettingsRepository settings;
    private final Audit audit;

    public PeriodCloser(PeriodRepository periods, ClaimRepository claims, PersonRepository people,
            SettingsRepository settings, Audit audit) {
        this.periods = periods;
        this.claims = claims;
        this.people = people;
        this.settings = settings;
        this.audit = audit;
    }

    public Period close(String sheetId, String by, Period found, boolean auto) {
        return close(sheetId, by, found, auto, false);
    }

    /** Close, or soft-close, a period (open or soft-closed), fixing its figures afresh. A soft close writes
     *  PERIOD_SOFT_CLOSED instead of PERIOD_CLOSED. */
    public Period close(String sheetId, String by, Period found, boolean auto, boolean soft) {
        LocalDate from = LocalDate.parse(found.from());
        LocalDate to = LocalDate.parse(found.to());
        String today = periods.today(sheetId);
        Map<Long, List<Hold>> holds = new java.util.HashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        Map<Long, BigDecimal> personCarried = new LinkedHashMap<>();
        List<Period> earlier = periods.list(sheetId).stream().filter(q -> LocalDate.parse(q.to()).isBefore(from)).toList();
        for (Person p : people.list(sheetId)) {
            personTotals.put(p.id(), BigDecimal.ZERO.setScale(2));
            personCarried.put(p.id(), BigDecimal.ZERO.setScale(2));
            holds.put(p.id(), p.liveHolds(today));
        }
        Map<Long, String> statusesThen = claims.statusesOn(sheetId, to);
        for (Claim c : claims.list(sheetId)) {
            String then = statusesThen.get(c.id());
            if (!c.countsAs(then, holds.getOrDefault(c.personId(), List.of()), today)) continue;
            LocalDate on = LocalDate.parse(c.dueBy());
            boolean in = !on.isBefore(from) && !on.isAfter(to);
            boolean carried = (then.equals("OPEN") || then.equals("PENDING")) && earlier.stream()
                    .anyMatch(q -> !on.isBefore(LocalDate.parse(q.from())) && !on.isAfter(LocalDate.parse(q.to())));
            if (!in && !carried) continue;
            BigDecimal now = claims.dueNow(sheetId, c);
            if (now == null) continue;
            if (in) {
                total = total.add(now);
                personTotals.merge(c.personId(), now, BigDecimal::add);
            }
            if (carried) personCarried.merge(c.personId(), now, BigDecimal::add);
        }
        Map<Long, BigDecimal> openings = periods.openings(sheetId, found.id());
        for (Map.Entry<Long, BigDecimal> e : openings.entrySet()) {
            if (personCarried.containsKey(e.getKey())) personCarried.merge(e.getKey(), e.getValue(), BigDecimal::add);
        }
        String shown = total.setScale(2).toPlainString();
        periods.close(sheetId, found.id(), total, personTotals, personCarried, soft);
        audit.record((soft ? "PERIOD_SOFT_CLOSED" : "PERIOD_CLOSED") + " sheet=" + sheetId + " by=" + by + " period=" + found.name() + " total=" + shown
                + (auto ? " auto=yes" : ""));
        return new Period(found.id(), found.name(), found.from(), found.to(), !soft, shown, personTotals.entrySet().stream().collect(Collectors.toMap(e -> Long.toString(e.getKey()),
                e -> e.getValue().setScale(2).toPlainString(), (a, b) -> a, LinkedHashMap::new)), personCarried.entrySet().stream().collect(Collectors.toMap(e -> Long.toString(e.getKey()),
                e -> e.getValue().setScale(2).toPlainString(), (a, b) -> a, LinkedHashMap::new)), soft, found.cap(),
                openings.entrySet().stream().collect(Collectors.toMap(e -> Long.toString(e.getKey()),
                e -> e.getValue().setScale(2).toPlainString(), (a, b) -> a, LinkedHashMap::new)), found.locked());
    }

    /** Close, by the sheet's grace days (none when blank), every open (not closed nor soft-closed) period whose to-date plus the grace days is
     *  before the sheet's today, in order of their to-dates. */
    public void closeLapsed(String sheetId, String by, LocalDate today) {
        Integer grace = settings.graceDays(sheetId);
        if (grace == null) return;
        List<Period> lapsed = periods.list(sheetId).stream()
                .filter(p -> !p.closed() && !p.softClosed() && LocalDate.parse(p.to()).plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(Period::to)).toList();
        for (Period p : lapsed) close(sheetId, by, p, true);
    }
}
