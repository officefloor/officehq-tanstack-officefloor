package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.plan.GenerateResponse;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * For every plan on the sheet, in the order added, a DRAFT claim with the plan's amount and an empty note for each plan
 * date (see Plan#datesThrough) up to its end and the sheet's today that has none yet, dated that day. Each is
 * checked as the claim form checks a new claim (closed period, retirement); a date it refuses is skipped, to be tried
 * again next time. Each added claim is recorded as CLAIM_ADDED, then the rollup lines for what the generate moved.
 */
public class GeneratePlanClaims {

    public void generate(@Val SheetResponse sheet, @Val ChangedBy by, PlanRepository plans, PersonRepository people,
            ClaimRepository claims, SheetRepository sheets, PeriodRepository periods, SettingRepository settings,
            HolidayRepository holidays, Rollup rollup, Audit audit, ClaimLog log, Out<GenerateResponse> response) {
        LocalDate today = sheets.today(sheet.id());
        RollupSnapshot before = rollup.snapshot(sheet.id());
        int made = 0;
        for (Plan plan : plans.list(sheet.id())) {
            Set<LocalDate> done = plans.made(plan.id());
            for (LocalDate date : plan.datesThrough(today)) {
                if (done.contains(date)) {
                    continue;
                }
                // Re-read each time: an earlier claim may have changed what the rules see.
                Person person = people.find(sheet.id(), plan.personId());
                try {
                    RefuseClosedDate.refuse(sheet.id(), BuildClaim.dueBy(person, date, settings, holidays), periods);
                    RefuseRetiredDate.refuse(person, date);
                } catch (BadRequestException e) {
                    continue;
                }
                Claim claim = claims.insert(sheet.id(), person.getId(), plan.amount(), date, "", null, null, null,
                        BuildClaim.terms(person, date, settings));
                if (periods.softClosedOn(sheet.id(), claim.getDueBy())) {
                    claims.markLate(claim);
                }
                plans.markMade(plan, date);
                audit.record(ClaimAudit.added(claim, by, person.getName()));
                if (claim.isLateEntry()) {
                    audit.record(ClaimAudit.line("LATE_ENTRY", claim, by, person.getName()));
                }
                log.record(claim, by);
                made++;
            }
        }
        rollup.record(sheet.id(), before, by);
        response.set(new GenerateResponse(made));
    }
}
