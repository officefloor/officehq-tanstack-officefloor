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

/** Close an open period: fix its due-total as shown now (the dues now, late charges included, of the claims dated in
 *  it that count by the status they had at the end of its last day, a person's open (OPEN or PENDING) claims dated
 *  within one of their holds left out unless bound by a statement, skipping those with none), so later changes to
 *  rates do not alter it; and each person's due-total for it likewise, for every person on the sheet (0.00 for one
 *  with nothing in it). Writes PERIOD_CLOSED, ending auto=yes when closed by the sheet's grace days. */
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
        Map<Long, List<Hold>> holds = people.holds(sheetId);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Person p : people.list(sheetId)) personTotals.put(p.id(), BigDecimal.ZERO.setScale(2));
        Map<Long, String> statusesThen = claims.statusesOn(sheetId, to);
        String today = periods.today(sheetId);
        for (Claim c : claims.list(sheetId)) {
            LocalDate on = LocalDate.parse(c.date());
            if (on.isBefore(from) || on.isAfter(to)
                    || !c.countsAs(statusesThen.get(c.id()), holds.getOrDefault(c.personId(), List.of()), today)) continue;
            BigDecimal now = claims.dueNow(sheetId, c);
            if (now != null) {
                total = total.add(now);
                personTotals.merge(c.personId(), now, BigDecimal::add);
            }
        }
        String shown = total.setScale(2).toPlainString();
        periods.close(sheetId, found.id(), total, personTotals, soft);
        audit.record((soft ? "PERIOD_SOFT_CLOSED" : "PERIOD_CLOSED") + " sheet=" + sheetId + " by=" + by + " period=" + found.name() + " total=" + shown
                + (auto ? " auto=yes" : ""));
        return new Period(found.id(), found.name(), found.from(), found.to(), !soft, shown, personTotals.entrySet().stream().collect(Collectors.toMap(e -> Long.toString(e.getKey()),
                e -> e.getValue().setScale(2).toPlainString(), (a, b) -> a, LinkedHashMap::new)), soft);
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
