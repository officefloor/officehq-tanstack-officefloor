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
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes the period, fixing its due-total now, and each person's on the sheet (the part of it from their claims): the due now of the claims dated in it
 * that counted at the end of its last day (OPEN, PENDING and SETTLED then by their status log, but not a deferred one, nor an unbound open one dated
 * within its person's hold) (a
 * claim with no rate adds nothing). Refuses a role the permission matrix does not let close, a period not on the sheet or one already closed.
 */
public class ClosePeriod {
    public void close(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, Claims claims, People people, Rates rates, Settings settings,
            SheetClock clock, Roles roles, Audit audit)
            throws InvalidPeriodException {
        if (!settings.permits(sheet, "close", roles.of(sheet, user))) {
            throw new InvalidPeriodException("Your role may not close a period");
        }
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
            if (person == null || !person.countsOn(claim, to) || date.isBefore(from) || date.isAfter(to)) {
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
