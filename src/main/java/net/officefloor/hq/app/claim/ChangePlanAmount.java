package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.plan.AmountRequest;
import net.officefloor.hq.app.plan.InvalidPlanException;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanResponse;
import net.officefloor.hq.app.plan.Plans;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Changes the plan's amount on the sheet's today, audited as PLAN_CHANGED: the claims it generates from then on take the new
 *  amount. Those it already generated that are still DRAFT take it too, each as the claim form would edit it (by its rules, keeping
 *  the claim's date, note, ref and tags), audited as CLAIM_EDITED; a draft the form would refuse, and a claim in any other status,
 *  keeps its amount. An amount the claim form refuses is refused. */
public class ChangePlanAmount {
    public void change(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId, @RequestBody AmountRequest request,
            Plans plans, People people, Claims claims, Sheets sheets, Periods periods, Settings settings, Rollup rollup, Audit audit,
            ObjectResponse<PlanResponse> response)
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
        editDrafts(plan, amount, request.user(), plans, people, claims, sheets, periods, rollup, audit);
        response.send(PlanResponse.from(plans.find(plan.id()), today));
    }

    /** Edits each DRAFT claim the plan generated (on its own sheet or the plan's target) to the amount, as the claim form would. */
    private static void editDrafts(Plan plan, BigDecimal amount, String user, Plans plans, People people, Claims claims, Sheets sheets,
            Periods periods, Rollup rollup, Audit audit) {
        String on = plan.target() != null ? plan.target() : plan.sheetId();
        RollupFigures before = null;
        ApplyClaim form = new ApplyClaim();
        for (long id : plans.claimIds(plan.id())) {
            Claim claim = claims.find(on, id);
            if (claim == null || claim.getStatus() != ClaimStatus.DRAFT) continue;
            if (before == null) before = rollup.figures(on);
            ValidClaim edit = new ValidClaim(amount, claim.getDate(), claim.getNote(), user, claim.getCorrects(), claim.getRef(), claim.getTags());
            try {
                form.apply(edit, claim, sheets, periods, people, claims);
            } catch (InvalidClaimException e) {
                continue; // the form would refuse this draft's edit: it keeps its amount
            }
            claims.save(claim);
            audit.recordOnCommit(ClaimAudit.line("CLAIM_EDITED", user, people.find(on, claim.getPersonId()), claim, claim.getNumber()));
        }
        if (before != null) rollup.recordChanges(on, user, before);
    }
}
