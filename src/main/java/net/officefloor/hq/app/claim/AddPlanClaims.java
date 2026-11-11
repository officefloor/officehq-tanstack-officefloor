package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.CheckClaimPeriod;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.CheckClaimRetired;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.plan.GenerateResponse;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * For every plan on the sheet, in the order they were added, a DRAFT claim with the plan's amount for
 * every plan date (its start, then every N days; or monthly on its day of the month) up to its end and the sheet's today that has none yet,
 * dated that day with an empty note. Each is checked as the claim form checks a claim (its due-by outside
 * a closed period, its date not after its person's retirement): a date refused is skipped, and tried again
 * next time. Each claim made is saved, logged and audited as one added by the form. Like a bulk action, a
 * viewer, or anyone on an archived sheet, goes through and makes none.
 * <p>
 * A plan naming a descendant sheet makes its claims there instead, for the person of the same name on that
 * sheet (none when there is no such person), by that sheet's rules (its terms, holidays, periods and today)
 * and in that sheet's audit, followed by that sheet's rollup lines; its dates still run up to this sheet's
 * today, and the plan still counts what it made. A descendant archived, or on which the user is a viewer,
 * gets none.
 */
public class AddPlanClaims {
    public void add(@Val Sheet sheet, PlanRepository plans, PersonRepository people, ClaimRepository claims,
            PeriodRepository periods, RateRepository rates, SettingsRepository settings, ClaimLogRepository log,
            HolidayRepository holidays, RoleRepository roles, SheetRepository sheets, HttpServletRequest request, Audit audit, Out<GenerateResponse> result) {
        Settings terms = settings.find(sheet.id());
        Set<LocalDate> holidayDates = holidays.dates(sheet.id());
        String user = Audit.user(request);
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new GenerateResponse(0)); // a viewer, or an archived sheet, makes nothing
            return;
        }
        int made = 0;
        Map<String, Rollup> touched = new LinkedHashMap<>();
        for (Plan plan : plans.findAll(sheet.id())) {
            Person owner = people.find(sheet.id(), plan.personId());
            if (owner == null) continue;
            Sheet on = sheet;
            Person person = owner;
            if (plan.target() != null) {
                on = sheets.find(plan.target());
                if (on == null || on.archived() || Role.VIEWER.equals(roles.roleOf(on.id(), user))) continue;
                person = people.findByName(on.id(), owner.getName());
                if (person == null) continue;
            }
            Settings onTerms = on == sheet ? terms : settings.find(on.id());
            Set<LocalDate> onHolidays = on == sheet ? holidayDates : holidays.dates(on.id());
            Set<LocalDate> done = plans.made(plan.id());
            LocalDate last = plan.end().isBefore(sheet.today()) ? plan.end() : sheet.today();
            for (LocalDate date : plan.datesUpTo(last)) {
                if (done.contains(date)) continue;
                Claim claim = new Claim(null, on.id(), person.getId(), plan.amount(), date, "",
                        ClaimStatus.DRAFT, null, null, null);
                claim.fixDueBy(onTerms.termsFor(person, on.today()), onHolidays);
                try {
                    new CheckClaimPeriod().check(on, claim, periods);
                    new CheckClaimRetired().check(on, claim, people);
                } catch (ClosedPeriodException | RetiredPersonException e) {
                    continue;
                }
                if (on != sheet && !touched.containsKey(on.id())) {
                    touched.put(on.id(), Rollup.of(on.id(), on.today(), people, claims, rates, onTerms));
                }
                claim.setLateEntry(periods.softClosedOn(on.id(), claim.getDueBy()) != null);
                claims.save(claim);
                plans.recordMade(plan.id(), date, claim.getId());
                log.record(claim, user, on.today(), LogClaimStatus.due(on, claim, people, rates, settings));
                new AuditClaimAdded().audit(on, person, claim, request, audit);
                made++;
            }
        }
        // Each descendant written to: the figures the plans moved there, in its own audit.
        for (Map.Entry<String, Rollup> e : touched.entrySet()) {
            Rollup after = Rollup.of(e.getKey(), sheets.today(e.getKey()), people, claims, rates,
                    settings.find(e.getKey()));
            after.changesSince(e.getValue(), e.getKey(), user).forEach(audit::record);
        }
        result.set(new GenerateResponse(made));
    }
}
