package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.plan.AmountRequest;
import net.officefloor.hq.app.plan.InvalidPlanException;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanResponse;
import net.officefloor.hq.app.plan.Plans;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Changes the plan's amount on the sheet's today, audited as PLAN_CHANGED: the claims it generates from then on take the new
 *  amount, those already generated keep theirs. An amount the claim form refuses is refused. */
public class ChangePlanAmount {
    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId, @RequestBody AmountRequest request,
            Plans plans, People people, Sheets sheets, Settings settings, Audit audit, ObjectResponse<PlanResponse> response)
            throws InvalidPlanException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        BigDecimal amount;
        try {
            amount = ValidateClaim.amount(request.amount());
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        Plan plan;
        try {
            plan = plans.find(Long.parseLong(planId));
        } catch (NumberFormatException e) {
            plan = null;
        }
        if (plan == null || !plan.sheetId().equals(sheetId)) {
            throw new InvalidPlanException("No such plan");
        }
        Person person = people.find(sheetId, plan.personId());
        LocalDate today = sheets.today(sheetId);
        plans.changeAmount(plan.id(), today, amount);
        audit.recordOnCommit("PLAN_CHANGED sheet=" + sheetId + " by=" + request.user() + " name=" + (person != null ? person.getName() : "")
                + " start=" + plan.start() + " amount=" + amount.toPlainString());
        response.send(PlanResponse.from(plans.find(plan.id()), today));
    }
}
