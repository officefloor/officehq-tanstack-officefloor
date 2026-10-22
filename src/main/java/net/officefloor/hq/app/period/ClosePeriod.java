package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Grades;
import net.officefloor.hq.app.person.Holds;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Close the named period, fixing its due-total, and each person's on the sheet, as they are now: the figures (as shown on their rows) of the claims
 *  that count (an OPEN one dated within a hold of its person, not bound by a statement, does not) whose date falls in it, skipping n/a. Refused when there is no such period, or it is already
 *  closed. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            PeriodRepository periods, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets,
            Out<Period> closed) throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        Period period = periods.find(id, periodName);
        if (period == null) {
            throw new InvalidPeriodException("There is no period " + periodName.trim() + " on this sheet.");
        }
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        Map<Long, Person> byId = people.list(id).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        BigDecimal floor = settings.floor(id);
        LateCharge late = LateCharge.of(id, settings, sheets, claims);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Long personId : byId.keySet()) {
            personTotals.put(personId, new BigDecimal("0.00"));
        }
        BigDecimal total = new BigDecimal("0.00");
        for (Claim c : claims.list(id)) {
            Person person = byId.get(c.personId());
            if (!Dues.counts(c, person != null ? person.holds() : Holds.NONE) || !period.contains(c.date())) {
                continue;
            }
            BigDecimal figure = Dues.shown(c, person != null ? person.grades() : Grades.NONE, rates, floor, late);
            if (figure != null) {
                total = total.add(figure);
                if (person != null) {
                    personTotals.merge(person.id(), figure, BigDecimal::add);
                }
            }
        }
        if (!periods.close(period, total, personTotals)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        closed.set(new Period(period.sheetId(), period.name(), period.from(), period.to(), true, total));
    }
}
