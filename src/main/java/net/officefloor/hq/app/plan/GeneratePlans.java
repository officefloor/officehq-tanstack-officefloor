package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * For every plan on the sheet, in the order added, adds a DRAFT claim of the plan's amount, dated that day with an
 * empty note, for each plan date (the start, then every N days; or a monthly plan's day in each month) up to the end and the sheet's today that has none
 * yet and no pause skips, as the claim form's BuildClaim adds one. A date the form would refuse (due inside a closed period, or after the
 * person's retirement) is skipped, and tried again next time. A viewer's generate makes none.
 * A plan naming a descendant sheet makes its claims on that sheet instead, for the person of the same name there (none
 * when there is none), by that sheet's rules and in its audit; it still counts what it made.
 */
public class GeneratePlans {
    public void generate(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Plans plans,
            People people, Claims claims, Periods periods, Settings settings, Holidays holidays, SheetClock clock,
            ClaimLog log, Audit audit, Rollup rollup, Roles roles, ObjectResponse<GeneratedPlans> response) {
        LocalDate today = clock.today(sheet);
        int made = 0;
        boolean viewer = Role.VIEWER.equals(roles.of(sheet, user));
        for (Plan plan : viewer ? List.<Plan>of() : plans.list(sheet)) {
            Person person = people.find(sheet, plan.person());
            if (person == null) {
                continue;
            }
            // A plan naming a descendant sheet makes its claims there, for the person of the same name, by its rules.
            String on = plan.sheet() == null ? sheet : plan.sheet();
            if (!on.equals(sheet)) {
                person = people.findByName(on, person.name());
                if (person == null) {
                    continue;
                }
            }
            LocalDate rulesToday = clock.today(on);
            LocalDate end = LocalDate.parse(plan.end());
            LocalDate last = end.isBefore(today) ? end : today;
            for (LocalDate day : plans.dates(plan, last)) {
                if (plans.made(plan.id(), day)) {
                    continue;
                }
                LocalDate dueBy = person.dueBy(on, day, rulesToday, settings, holidays);
                if (periods.closedOn(on, dueBy) || person.retiredBefore(day)) {
                    continue;
                }
                ClaimInput input = new ClaimInput(new BigDecimal(plan.amount()), day, "");
                Rollup.Snapshot before = rollup.take(on);
                long id = claims.add(on, person.id(), input, dueBy, person.termsOn(on, rulesToday, settings));
                log.record(on, user, id);
                plans.recordMade(plan.id(), day, id);
                audit.record("CLAIM_ADDED sheet=" + on + " by=" + user + " " + input.addedAuditFields(person.name()));
                if (periods.softClosedOn(on, dueBy)) {
                    claims.setLateEntry(on, id);
                    audit.record("LATE_ENTRY sheet=" + on + " by=" + user + " " + input.auditFields(person.name()));
                }
                rollup.record(on, user, before);
                made++;
            }
        }
        response.send(new GeneratedPlans(made, claims.list(sheet), plans.list(sheet)));
    }
}
