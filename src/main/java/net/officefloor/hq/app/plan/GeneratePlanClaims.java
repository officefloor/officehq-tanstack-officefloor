package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimAdding;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * For every plan on the sheet in the order they were added, adds a DRAFT claim of the plan's amount, dated that
 * day with an empty note, for every plan date (the start, then every N days, or a monthly plan's day in each month) up to its end and the sheet's today
 * that it has made none for yet, as the claim form would (recorded as CLAIM_ADDED). A plan naming a descendant
 * sheet makes them for the person of the same name on that sheet (none when there is none), by that sheet's rules
 * and recorded in its audit. A date the claim form refuses
 * is skipped, and tried again on the next generate. A viewer makes none: the answer is 0, storing and recording nothing.
 */
public class GeneratePlanClaims {
    public void generate(@PathVariable("sheetId") String sheetId, @Val PlanGenerateRequest request, PlanStore plans,
            PersonStore people, SheetStore sheets, ClaimStore claims, SettingStore settings, ClaimLogStore log,
            PeriodStore periods, RollupService rollups, Audit audit, HolidayStore holidays, RoleStore roles,
            Out<PlanGenerateResult> result) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        int made = 0;
        if (roles.find(sheet, request.user()) == Role.VIEWER) {
            result.set(new PlanGenerateResult(made));
            return;
        }
        for (Plan plan : plans.list(sheet)) {
            LocalDate last = plan.end().isBefore(today) ? plan.end() : today;
            for (LocalDate d : plan.dates(last)) {
                if (plans.made(plan.id(), d)) {
                    continue;
                }
                String on = plan.targetSheet() == null ? sheet : plan.targetSheet();
                Person person = people.find(sheet, plan.personId());
                if (person != null && plan.targetSheet() != null) {
                    person = people.findByName(on, person.getName());
                }
                if (person == null) {
                    break;
                }
                ClaimInput input;
                try {
                    input = ClaimAdding.check(on, person, new ClaimRequest(plan.amount().toPlainString(),
                            d.toString(), "", request.user(), "", ""), settings, periods, claims, holidays);
                } catch (InvalidSheetException e) {
                    continue;
                }
                ClaimAdding.add(input, person, claims, settings, log, periods, rollups, audit, holidays);
                plans.markMade(plan.id(), d);
                made++;
            }
        }
        result.set(new PlanGenerateResult(made));
    }
}
