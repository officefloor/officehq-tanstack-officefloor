package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes an open period, fixing its due-total as it is now: the figures of the claims that count (a held person's OPEN claims do not) whose date falls
 *  in it, each with its late charge, added up (skipping n/a). Closing a period already closed is refused. */
public class ClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods,
            ClaimRepository claims, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets,
            Out<PeriodClose> closed)
            throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        Map<Long, Person> byId = people.list(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, (p) -> p));
        BigDecimal floor = settings.floor(period.sheetId());
        LateCharge late = LateCharge.of(period.sheetId(), sheets, settings);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.list(period.sheetId())) {
            Person person = byId.get(c.personId());
            if (!ClaimDue.countsLive(c, person != null && person.held()) || c.date().isBefore(period.from()) || c.date().isAfter(period.to())) {
                continue;
            }
            BigDecimal figure = person == null ? c.due() : late.add(c, ClaimDue.figure(c, person.grade(), rates, floor));
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
