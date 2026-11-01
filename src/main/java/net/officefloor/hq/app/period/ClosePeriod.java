package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes the period, fixing its due-total now, and each person's on the sheet (the part of it from their claims): the due of the counted (OPEN and SETTLED, but not an unbound OPEN one dated
 * within its person's hold) claims dated in it (a
 * claim with no rate adds nothing). Refuses a period not on the sheet or already closed.
 */
public class ClosePeriod {
    public void close(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, Claims claims, People people, Rates rates, Settings settings,
            SheetClock clock, Audit audit)
            throws InvalidPeriodException {
        Period found = periods.find(sheet, period);
        if (found == null) {
            throw new InvalidPeriodException("There is no period named " + period);
        }
        if (found.closed()) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
        LocalDate from = LocalDate.parse(found.from());
        LocalDate to = LocalDate.parse(found.to());
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, (p) -> p));
        List<Rate> sheetRates = rates.list(sheet);
        Dated floor = settings.floor(sheet);
        Late late = settings.late(sheet, clock.today(sheet));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> perPerson = new LinkedHashMap<>();
        byId.keySet().forEach((id) -> perPerson.put(id, BigDecimal.ZERO.setScale(2)));
        for (Claim claim : claims.list(sheet)) {
            LocalDate date = LocalDate.parse(claim.date());
            Person person = byId.get(claim.person());
            if (person == null || !person.counts(claim) || date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            BigDecimal due = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
            if (due != null) {
                total = total.add(due);
                perPerson.merge(person.id(), due, BigDecimal::add);
            }
        }
        if (!periods.close(sheet, period, total, perPerson)) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
        audit.record("PERIOD_CLOSED sheet=" + sheet + " by=" + user + " period=" + period + " total="
                + total.toPlainString());
    }
}
