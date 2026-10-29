package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimAfterRetirementException;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimInClosedPeriodException;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class GeneratePlanClaims {
    // For every plan of the sheet, in the order they were added, and each of its dates (the start, then every N days; or the day of every month) up to
    // its end and the sheet's today that it has not made a claim for yet: a DRAFT claim of the plan's amount dated that day
    // with an empty note, added as the claim form adds one and recorded as CLAIM_ADDED. A date the claim form refuses
    // (due by inside a closed period, after the person's retirement) is skipped, and tried again next time. A viewer is
    // refused at the first claim there is to make, before anything is stored; with none to make, they are told 0.
    public void generate(@PathVariable("sheetId") String sheetId, Plans plans, People people, Claims claims, Sheets sheets,
            Settings settings, Holidays holidays, Periods periods, Rollup rollup, ServerHttpConnection connection,
            Audit audit, Out<GenerateResponse> result) throws ViewerRefusedException {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        LocalDate today = sheets.today(sheet);
        int made = 0;
        for (Plan plan : plans.list(sheet)) {
            Person person = people.find(sheet, plan.personId());
            if (person == null) {
                continue;
            }
            Set<LocalDate> done = new HashSet<>(plans.made(plan.id()));
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate date : plan.dates(last)) {
                if (done.contains(date)) {
                    continue;
                }
                int terms = person.termsOn(date, settings);
                LocalDate dueBy = holidays.dueBy(sheet, date, terms);
                try {
                    ClaimFormRules.refuseClosed(periods, sheet, dueBy);
                    ClaimFormRules.refuseRetired(person, date);
                } catch (ClaimInClosedPeriodException | ClaimAfterRetirementException e) {
                    continue;
                }
                if (settings.isViewer(sheet, user)) {
                    throw new ViewerRefusedException("A viewer can change nothing.");
                }
                Claim claim = claims.add(sheet, person.id(), plan.amount(), date, "", null, dueBy, terms, null, List.of());
                claims.log(claim, user.trim(), today, rollup.figure(claim));
                plans.markMade(plan.id(), date, claim.id());
                ClaimResponse shown = ClaimResponse.from(claim);
                audit.record(Audit.line("CLAIM_ADDED", sheet, user, "person=" + person.name(), "amount=" + shown.amount(),
                        "date=" + shown.date()));
                if (claim.lateEntry()) {
                    audit.record(Audit.line("LATE_ENTRY", sheet, user, "person=" + person.name(),
                            "amount=" + shown.amount(), "date=" + shown.date()));
                }
                made++;
            }
        }
        result.set(new GenerateResponse(made));
    }
}
