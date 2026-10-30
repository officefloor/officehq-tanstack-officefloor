package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.plan.GenerateRequest;
import net.officefloor.hq.app.plan.GenerateResponse;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.Plans;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * For every plan on the sheet, in the order they were added, makes a DRAFT claim of the plan's amount for each plan date (the
 * start, then every N days; or the day in every month from the start's month, on the month's last day when it is shorter,
 * dates before the start left out) up to the end and the sheet's today that has none yet, dated that day with an empty note, each
 * audited as CLAIM_ADDED. A date the claim form refuses (its due-by inside a closed period, after the person's retirement) is
 * skipped, and tried again on the next generate. A viewer changes nothing: their generate makes no claim (an archived sheet
 * refuses it).
 */
public class GeneratePlanClaims {
    public void generate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request, Plans plans, People people,
            Claims claims, Sheets sheets, Periods periods, Holidays holidays, Settings settings, ClaimLog log, Audit audit,
            @Val RollupFigures before, Rollup rollup, ObjectResponse<GenerateResponse> response) {
        try {
            settings.refuseViewer(sheetId, request.user());
        } catch (ViewerRefusedException e) {
            response.send(new GenerateResponse(0));
            return;
        }
        LocalDate today = sheets.today(sheetId);
        int made = 0;
        for (Plan plan : plans.list(sheetId)) {
            Person person = people.find(sheetId, plan.personId());
            if (person == null) continue;
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate date : plan.dates(last)) {
                if (plans.made(plan.id(), date)) continue;
                LocalDate dueBy = holidays.dueBy(sheetId, date, settings.termsOnCreation(sheetId, person));
                if (periods.isClosed(sheetId, dueBy) || !person.allowsClaimOn(date)) continue;
                Claim claim = claims.add(sheetId, person.getId(), plan.amount(), date, "", null, dueBy, null);
                plans.recordMade(plan.id(), date, claim.getId());
                log.record(claim, person, request.user());
                audit.recordOnCommit(ClaimAudit.added(request.user(), person, claim));
                LateEntry.mark(claim, person, request.user(), claims, periods, audit);
                made++;
            }
        }
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(new GenerateResponse(made));
    }
}
