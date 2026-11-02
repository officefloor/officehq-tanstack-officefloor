package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * For every plan on the sheet, in the order added, adds a DRAFT claim of the plan's amount, dated that day with an
 * empty note, for each plan date (the start, then every N days) up to the end and the sheet's today that has none
 * yet, as the claim form's BuildClaim adds one. A date the form would refuse (due inside a closed period, or after the
 * person's retirement) is skipped, and tried again next time.
 */
public class GeneratePlans {
    public void generate(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Plans plans,
            People people, Claims claims, Periods periods, Settings settings, Holidays holidays, SheetClock clock,
            ClaimLog log, Audit audit, Rollup rollup, ObjectResponse<GeneratedPlans> response) {
        LocalDate today = clock.today(sheet);
        int made = 0;
        for (Plan plan : plans.list(sheet)) {
            Person person = people.find(sheet, plan.person());
            if (person == null) {
                continue;
            }
            LocalDate end = LocalDate.parse(plan.end());
            LocalDate last = end.isBefore(today) ? end : today;
            for (LocalDate day = LocalDate.parse(plan.start()); !day.isAfter(last); day = day.plusDays(plan.every())) {
                if (plans.made(plan.id(), day)) {
                    continue;
                }
                LocalDate dueBy = person.dueBy(sheet, day, settings, holidays);
                if (periods.closedOn(sheet, dueBy) || person.retiredBefore(day)) {
                    continue;
                }
                ClaimInput input = new ClaimInput(new BigDecimal(plan.amount()), day, "");
                Rollup.Snapshot before = rollup.take(sheet);
                long id = claims.add(sheet, person.id(), input, dueBy, person.termsOn(sheet, day, settings));
                log.record(sheet, user, id);
                plans.recordMade(plan.id(), day, id);
                audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.addedAuditFields(person.name()));
                if (periods.softClosedOn(sheet, dueBy)) {
                    claims.setLateEntry(sheet, id);
                    audit.record("LATE_ENTRY sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
                }
                rollup.record(sheet, user, before);
                made++;
            }
        }
        response.send(new GeneratedPlans(made, claims.list(sheet), plans.list(sheet)));
    }
}
