package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.RecordClaimAdded;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** For every plan on the sheet, in the order added, a DRAFT claim of the plan's amount for each plan
 *  date (the start, then every N days; or the day of the month in every month) up to its end and the sheet's today that it has not made yet,
 *  dated that day with an empty note, by the claim form's rules: a date the form refuses is skipped,
 *  and tried again the next time. Each claim is saved and audited as an added claim. A viewer makes none. */
public class GeneratePlans {
    public void generate(@PathVariable("sheetId") String sheetId, @Val Actor actor, Plans plans, People people,
            Sheets sheets, ClosedPeriods closedPeriods, Claims claims, Settings settings, Holidays holidays,
            ClaimLog log, Audit audit, Out<GeneratePlansResult> result) {
        if (settings.role(sheetId, actor.user()) == net.officefloor.hq.app.settings.Role.VIEWER) {
            result.set(new GeneratePlansResult(0)); // a viewer changes nothing
            return;
        }
        LocalDate today = sheets.today(sheetId);
        RecordClaimAdded record = new RecordClaimAdded();
        Map<Long, Person> persons = new HashMap<>();
        int made = 0;
        for (Plan plan : plans.onSheet(sheetId)) {
            Person person = persons.computeIfAbsent(plan.personId(), id -> people.find(sheetId, id));
            if (person == null) continue;
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate date : plan.dates(last)) {
                if (plans.made(plan.id(), date)) continue;
                ClaimValues values = new ClaimValues(plan.amount(), date, "", null, actor.user(), null, null);
                Claim claim;
                try {
                    claim = ClaimForm.built(values, person, sheets, closedPeriods, claims, settings, holidays);
                } catch (Exception refused) {
                    continue;
                }
                claims.save(claim);
                plans.markMade(sheetId, plan.id(), date);
                record.record(values, person, claim, log, audit);
                made++;
            }
        }
        result.set(new GeneratePlansResult(made));
    }
}
