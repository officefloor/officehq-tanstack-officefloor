package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
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
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * For every plan on the sheet, in the order they were added, a DRAFT claim with the plan's amount for
 * every plan date (its start, then every N days) up to its end and the sheet's today that has none yet,
 * dated that day with an empty note. Each is checked as the claim form checks a claim (its due-by outside
 * a closed period, its date not after its person's retirement): a date refused is skipped, and tried again
 * next time. Each claim made is saved, logged and audited as one added by the form.
 */
public class AddPlanClaims {
    public void add(@Val Sheet sheet, PlanRepository plans, PersonRepository people, ClaimRepository claims,
            PeriodRepository periods, RateRepository rates, SettingsRepository settings, ClaimLogRepository log,
            HolidayRepository holidays, HttpServletRequest request, Audit audit, Out<GenerateResponse> result) {
        Settings terms = settings.find(sheet.id());
        Set<LocalDate> holidayDates = holidays.dates(sheet.id());
        String user = Audit.user(request);
        int made = 0;
        for (Plan plan : plans.findAll(sheet.id())) {
            Person person = people.find(sheet.id(), plan.personId());
            if (person == null) continue;
            Set<LocalDate> done = plans.made(plan.id());
            LocalDate last = plan.end().isBefore(sheet.today()) ? plan.end() : sheet.today();
            for (LocalDate date = plan.start(); !date.isAfter(last); date = date.plusDays(plan.every())) {
                if (done.contains(date)) continue;
                Claim claim = new Claim(null, sheet.id(), person.getId(), plan.amount(), date, "",
                        ClaimStatus.DRAFT, null, null, null);
                claim.fixDueBy(terms.termsFor(person, date), holidayDates);
                try {
                    new CheckClaimPeriod().check(sheet, claim, periods);
                    new CheckClaimRetired().check(sheet, claim, people);
                } catch (ClosedPeriodException | RetiredPersonException e) {
                    continue;
                }
                claim.setLateEntry(periods.softClosedOn(sheet.id(), claim.getDueBy()) != null);
                claims.save(claim);
                plans.recordMade(plan.id(), date, claim.getId());
                log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
                new AuditClaimAdded().audit(sheet, person, claim, request, audit);
                made++;
            }
        }
        result.set(new GenerateResponse(made));
    }
}
