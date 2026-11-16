package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** For every plan on the sheet in the order added, adds a DRAFT claim of the plan's amount, dated the day with an empty
 *  note, for each plan date (see PlanDates) up to its end and the sheet's today that the plan has made
 *  none for yet, as AddClaim adds one: a date the claim form's rules refuse (see ClaimRules) is skipped, to be tried
 *  again next time. Writes CLAIM_ADDED for each (then LATE_ENTRY for a late entry), then the figures the generate moved
 *  (see Rollup). Publishes how many were made. */
public class GeneratePlans {
    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request,
            @Val RollupBefore before, PlanRepository plans, PersonRepository people, ClaimRepository claims,
            SettingRepository settings, PeriodRepository periods, HolidayRepository holidays, SheetRepository sheets,
            ClaimLog log, Rollup rollup, Audit audit, Out<GenerateResult> result) {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        LocalDate today = sheets.today(sheet);
        int made = 0;
        for (Plan plan : plans.list(sheet, null)) {
            Person person = people.find(sheet, plan.personId());
            if (person == null) {
                continue;
            }
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate date : PlanDates.upTo(plan, last)) {
                if (plans.made(plan.id(), date)) {
                    continue;
                }
                ClaimValues values = new ClaimValues(plan.amount(), date, "", null, by, null, null);
                ClaimRules.Checked checked;
                try {
                    checked = ClaimRules.check(person, values, settings, periods, claims, holidays);
                } catch (InvalidClaimException e) {
                    continue;
                }
                Claim added = claims.add(sheet, person.id(), values.amount(), checked.date(), values.note(),
                        checked.corrects(), null, null, checked.dueBy(), checked.terms());
                log.write(added, by);
                plans.recordMade(sheet, plan.id(), date, added.id());
                audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + by + " person=" + person.name()
                        + " amount=" + added.amount().toPlainString() + " date=" + added.date());
                if (periods.softClosedOn(sheet, checked.dueBy())) {
                    claims.markLateEntry(added);
                    audit.record("LATE_ENTRY sheet=" + sheet + " by=" + by + " person=" + person.name()
                            + " amount=" + added.amount().toPlainString() + " date=" + added.date());
                }
                made++;
            }
        }
        rollup.record(sheet, by, before);
        result.set(new GenerateResult(made));
    }
}
