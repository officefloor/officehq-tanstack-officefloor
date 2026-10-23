package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** For every plan on the sheet, in the order they were added, make a DRAFT claim of the plan's amount, dated that day
 *  with an empty note, for every plan date (the start, then every N days; or a monthly plan's day in each month) up to the plan's end and the sheet's today
 *  that the plan has made none for yet. Each is added as the claim form adds one — its CLAIM_ADDED line and then how
 *  the sheet's figures moved over it — and a date the claim form would refuse (its rules, or after the person's
 *  retirement date, or a due-by inside, or a date and due-by straddling, a period closed by hand) is skipped, to be tried again next time. */
public class GeneratePlanClaims {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GenerateRequest request, PlanRepository plans,
            PersonRepository people, SheetRepository sheets, SettingRepository settings, HolidayRepository holidays,
            PeriodRepository periods, ClaimRepository claims, ClaimLogRepository log, Rollups rollups, Audit audit,
            Out<GenerateResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        LocalDate today = sheets.today(sheet);
        int made = 0;
        for (Plan plan : plans.list(sheet)) {
            Person person = people.find(sheet, plan.personId());
            if (person == null || today == null) {
                continue;
            }
            for (LocalDate date : plan.dates(today)) {
                if (plans.made(plan.id(), date)) {
                    continue;
                }
                ClaimInput claim;
                try {
                    claim = ClaimFormRules.check(new ClaimRequest(request.user(), plan.amount().toPlainString(),
                            date.toString(), "", "", "", null));
                    RetiredPersonException.refuse(person, date);
                } catch (InvalidClaimException | RetiredPersonException e) {
                    continue;
                }
                if (periods.closedByHandFor(sheet, date, DueBy.of(person, date, settings, sheets, holidays)) != null) {
                    continue;
                }
                Rollup before = rollups.take(sheet);
                Claim added = ClaimFormRules.add(claim, person, sheets, settings, holidays, periods, claims, log, audit);
                rollups.record(sheet, request.user(), before);
                plans.made(plan.id(), date, added.id());
                made++;
            }
        }
        result.set(new GenerateResponse(made));
    }
}
