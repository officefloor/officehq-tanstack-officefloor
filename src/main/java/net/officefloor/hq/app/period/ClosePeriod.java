package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes an open period, fixing its due-total as it is now: the figures (each due as shown now, with its late charge)
 *  of the claims whose date falls in it and that counted by the status each had at the end of the period's last day,
 *  as its status log records it (a claim added after it does not count), the live rules otherwise applying (a
 *  person's OPEN or PENDING claims dated within a hold of theirs, not bound by a statement, do not, nor do deferred
 *  claims), added up (skipping n/a). Each person's own due-total for the period is fixed
 *  too, for every person on the sheet; the period's is the sum of theirs. Closing a period already closed is refused. */
public class ClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods,
            ClaimRepository claims, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets, ClaimLogRepository log,
            Out<PeriodClose> closed)
            throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        Map<Long, Person> byId = people.list(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, (p) -> p));
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(period.sheetId());
        LateCharge late = LateCharge.of(period.sheetId(), sheets, settings, claims);
        Map<Long, BigDecimal> perPerson = new LinkedHashMap<>();
        for (Long id : byId.keySet()) {
            perPerson.put(id, BigDecimal.ZERO.setScale(2));
        }
        for (Claim c : claims.list(period.sheetId())) {
            Person person = byId.get(c.personId());
            if (c.date().isBefore(period.from()) || c.date().isAfter(period.to())) {
                continue;
            }
            ClaimStatus status = ClaimDue.statusAsOf(logs.getOrDefault(c.id(), List.of()), period.to());
            if (status == null || !ClaimDue.countsLive(ClaimDue.withStatus(c, status), person)
                    || ClaimDue.deferred(c, late.today())) {
                continue;
            }
            BigDecimal figure = person == null ? c.due() : late.add(c, ClaimDue.figure(c, person.gradeOn(c.date()), rates, settings.floor(c.sheetId(), c.date()), late.rounding()));
            if (figure != null) {
                perPerson.merge(c.personId(), figure, BigDecimal::add);
            }
        }
        BigDecimal total = perPerson.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!periods.close(period, total)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        periods.fixPersonTotals(period, perPerson);
        closed.set(new PeriodClose(request.by(), total));
    }
}
